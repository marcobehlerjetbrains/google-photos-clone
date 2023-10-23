package com.jetbrains.marcocodes.googlephotosclone;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.imaging.png.PngChunkType;
import com.drew.lang.GeoLocation;
import com.drew.metadata.Metadata;
import com.drew.metadata.MetadataException;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.drew.metadata.exif.GpsDirectory;
import com.drew.metadata.jpeg.JpegDirectory;
import com.drew.metadata.png.PngDirectory;
import jakarta.persistence.EntityManagerFactory;
import me.tongfei.progressbar.ProgressBar;
import me.tongfei.progressbar.ProgressBarBuilder;
import me.tongfei.progressbar.ProgressBarStyle;
import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class MediaScanner {


    private final Logger logger = LoggerFactory.getLogger(MediaScanner.class);

    private final Queries queries_;
    private final ImageMagick imageMagick;
    private final HttpClient client = HttpClient.newHttpClient();

    private final EntityManagerFactory emf;

    private String userHome = System.getProperty("user.home");
    private Path thumbnailsDir = Path.of(userHome).resolve(".photos");

    public MediaScanner(Queries queries, ImageMagick imageMagick, EntityManagerFactory emf) {
        queries_ = queries;
        this.imageMagick = imageMagick;
        this.emf = emf;
    }

    public int fullscan(Path sourceDir) {
        AtomicInteger counter = new AtomicInteger();

        long mediaCount;

        try (Stream<Path> media = Files.walk(sourceDir)
                .parallel()
                .filter(Files::isRegularFile)
                .filter(this::isImage)) {
            mediaCount = media.count();
        } catch (IOException e) {
            logger.error("Error getting media count", e);
            return 0;
        }


        ProgressBar pb = new ProgressBarBuilder()
                .setInitialMax(mediaCount)
                //.hideEta()
                .setTaskName("Full Scan")
                .setStyle(ProgressBarStyle.ASCII)
                //.setConsumer(new DelegatingProgressBarConsumer(logger::info))
                .build();

        try (pb) { // name, initial max

            ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
            try (Stream<Path> scannedMedia = Files.walk(sourceDir)
                    .parallel()
                    .filter(f -> Files.isRegularFile(f) && isImage(f))
            ) {


                //ArrayList<Path> media = scannedMedia.collect(Collectors.toCollection(ArrayList::new));
                // try-with-resource block

                // IntStream.range(0, media.size()/50+1).mapToObj(chunkNum -> media.subList(chunkNum*50, Math.min(media.size(), chunkNum*50+50))).parallel().forEach(50iesBatch -. {);


                scannedMedia.forEach(image -> executorService.submit(() -> {
                    String hash = hash(image);
                    if (hash == null) {
                        System.err.println("Could not compute hash for image : " + image.toAbsolutePath().toString());
                        return;
                    }

                    String filename = image.getFileName().toString();

                    if (!queries_.existsByFilenameAndHash(filename, hash)) {
                        Path thumbnail = getThumbnailPath(hash);

                        if (!Files.exists(thumbnail)) {
                            final boolean success = imageMagick.createThumbnail(image, thumbnail);
                            if (!success) {
                                pb.step();
                                System.err.println("Error creating thumbnail");
                                return;
                            }
                        }

                        try (InputStream is = Files.newInputStream(image)) {
                            Metadata metadata = ImageMetadataReader.readMetadata(is);
                            Location location = getLocation(metadata);
                            LocalDateTime creationTime = getCreationTime(image, metadata);
                            emf.unwrap(SessionFactory.class).inStatelessSession(ss -> {
                                Media media = null;
                                media = new Media(hash, filename, creationTime, image.toUri().toString(), location);
                                ss.insert(media);
                            });
                            counter.incrementAndGet();

                        } catch (ImageProcessingException e) {
                            e.printStackTrace();
                            // not an image or something else
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }

                    pb.step();
                }));
            } catch (IOException e) {
                logger.error("Error converting media count", e);
                return 0;
            }

            try {
                executorService.shutdown();
                executorService.awaitTermination(3, TimeUnit.HOURS);
            } catch (InterruptedException e) {
                // nothing to do here, just silently ignore
            }
        }

        return counter.get();
    }

    private boolean isImage(Path path) {
        try {
            String mimeType = Files.probeContentType(path);
            return mimeType != null && mimeType.contains("image");
        } catch (IOException e) {
            return false;
        }
    }

    Location getLocation(Metadata metadata) {
        GpsDirectory gpsDirectory = metadata.getFirstDirectoryOfType(GpsDirectory.class);
        if (gpsDirectory == null || gpsDirectory.getGeoLocation() == null) {
            return null;
        }

        GeoLocation geoLocation = gpsDirectory.getGeoLocation();
        if (geoLocation == null) {
            return null;
        }

        double latitude = geoLocation.getLatitude();
        double longitude = geoLocation.getLongitude();
        String dms = geoLocation.toDMSString();
        AtomicReference<String> state = new AtomicReference<>("UNKNOWN");
        AtomicReference<String> city = new AtomicReference<>("UNKNOWN");

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.3geonames.org/" + latitude + "," + longitude))
                .build();
        client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenAccept(xml -> {
                    state.set(xml.substring(xml.indexOf("<state>") + 7, xml.indexOf("</state>")));
                    city.set(xml.substring(xml.indexOf("<city>") + 6, xml.indexOf("</city>")));
                })
                .join();
        return new Location(latitude, longitude, state.get(), city.get(), dms);
    }

    record Dimensions(int width, int height) {
    }


    Dimensions getDimensions(Metadata metadata) {
        try {
            ExifIFD0Directory exifIfD0Directory = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
            if (exifIfD0Directory != null && exifIfD0Directory.containsTag(ExifIFD0Directory.TAG_IMAGE_WIDTH) && exifIfD0Directory.containsTag(ExifIFD0Directory.TAG_IMAGE_HEIGHT)) {
                int width = exifIfD0Directory.getInt(ExifIFD0Directory.TAG_IMAGE_WIDTH);
                int height = exifIfD0Directory.getInt(ExifIFD0Directory.TAG_IMAGE_HEIGHT);
                return new Dimensions(width, height);
            }

            PngDirectory pngDirectory = metadata.getFirstDirectoryOfType(PngDirectory.class);
            if (pngDirectory != null && pngDirectory.getPngChunkType().equals(PngChunkType.IHDR)) {
                int width = pngDirectory.getInt(PngDirectory.TAG_IMAGE_WIDTH);
                int height = pngDirectory.getInt(PngDirectory.TAG_IMAGE_HEIGHT);
                return new Dimensions(width, height);
            }

            JpegDirectory jpegDirectory = metadata.getFirstDirectoryOfType(JpegDirectory.class);
            if (jpegDirectory != null && jpegDirectory.containsTag(JpegDirectory.TAG_IMAGE_WIDTH) && jpegDirectory.containsTag(JpegDirectory.TAG_IMAGE_HEIGHT)) {
                int width = jpegDirectory.getInt(JpegDirectory.TAG_IMAGE_WIDTH);
                int height = jpegDirectory.getInt(JpegDirectory.TAG_IMAGE_HEIGHT);
                return new Dimensions(width, height);
            }
        } catch (MetadataException e) {
            e.printStackTrace();
        }
        return new Dimensions(0, 0);
    }

    LocalDateTime getCreationTime(Path image, Metadata metadata) {

        ExifSubIFDDirectory exifSubIFDDirectory = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
        if (exifSubIFDDirectory != null && exifSubIFDDirectory.containsTag(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL)) {
            Date creatioDate = exifSubIFDDirectory.getDate(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL);
            return creatioDate.toInstant().atOffset(ZoneOffset.UTC).toLocalDateTime();
        }


        ExifIFD0Directory exifIFD0Directory = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
        if (exifIFD0Directory != null && exifIFD0Directory.containsTag(ExifIFD0Directory.TAG_DATETIME)) {
            Date creatioDate = exifIFD0Directory.getDate(ExifIFD0Directory.TAG_DATETIME);
            return creatioDate.toInstant().atOffset(ZoneOffset.UTC).toLocalDateTime();
        }


        GpsDirectory firstDirectoryOfType = metadata.getFirstDirectoryOfType(GpsDirectory.class);
        if (firstDirectoryOfType != null && firstDirectoryOfType.getGpsDate() != null) {
            Date gpsDate = firstDirectoryOfType.getGpsDate();
            return gpsDate.toInstant().atOffset(ZoneOffset.UTC).toLocalDateTime();
        }
        try {
            BasicFileAttributes attr = Files.readAttributes(image, BasicFileAttributes.class);
            FileTime fileTime = attr.creationTime();
            return LocalDateTime.ofInstant(fileTime.toInstant(), ZoneId.systemDefault());
        } catch (IOException e) {
            e.printStackTrace();
            return LocalDateTime.now();
        }
    }


    private Path getThumbnailPath(String hash) {
        String dir = hash.substring(0, 2);
        String filename = hash.substring(2);

        Path storageDir = thumbnailsDir.resolve(dir);
        if (!Files.exists(storageDir)) {
            try {
                Files.createDirectories(storageDir);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return storageDir.resolve(filename + ".webp");
    }


    public static String hash(Path file) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream ios = Files.newInputStream(file)) {
                int BUFFER_SIZE = 8192;
                while (true) {
                    byte[] buffer1 = new byte[BUFFER_SIZE];
                    int read = ios.readNBytes(buffer1, 0, BUFFER_SIZE);
                    if (read > 0) {
                        digest.update(buffer1);
                    }
                    if (read < BUFFER_SIZE) {
                        break;
                    }
                }
                byte[] digestedBytes = digest.digest();
                BigInteger bi = new BigInteger(1, digestedBytes);
                return String.format("%0" + (digestedBytes.length << 1) + "x", bi);
            } catch (IOException e) {
                return null;
            }
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
