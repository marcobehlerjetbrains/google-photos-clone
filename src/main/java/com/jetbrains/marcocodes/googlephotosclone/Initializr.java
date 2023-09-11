package com.jetbrains.marcocodes.googlephotosclone;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.exif.GpsDirectory;
import io.github.rctcwyvrn.blake3.Blake3;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

@Component
public class Initializr implements ApplicationRunner {

    static String userHome = System.getProperty("user.home");
    static Path thumbnailsDir = Path.of(userHome).resolve(".photos");
    private final Queries queries_;
    private final ImageMagick imageMagick;

    private final EntityManagerFactory emf;


    public Initializr(Queries queries_, ImageMagick imageMagick, EntityManagerFactory emf) {
        this.queries_ = queries_;
        this.imageMagick = imageMagick;
        this.emf = emf;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        Files.createDirectories(thumbnailsDir);

        String directory = args.getSourceArgs().length == 1 ? args.getSourceArgs()[0] : ".";
        Path sourceDir = Path.of(directory);

        AtomicInteger counter = new AtomicInteger();
        long start = System.currentTimeMillis();

        ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        try (Stream<Path> images = Files.walk(sourceDir)
                .filter(Files::isRegularFile)
                .filter(Initializr::isImage);
        ) {
            images.forEach(image -> executorService.submit(() -> {
                emf.unwrap(SessionFactory.class).inTransaction(em -> {
                    try {
                        String hash = hash(image);
                        String filename = image.getFileName().toString();

                        if (!queries_.existsByFilenameAndHash(filename, hash)) {
                            final boolean success = createThumbnail(image, hash);
                            if (success) {
                                counter.incrementAndGet();
                                Media media = new Media(hash, filename, creationTime(image));
                                Media.GPSCoordinates gpsCoordinates = gpsCoordinates(image);
                                if (gpsCoordinates != null) {
                                    media.setLatitude(gpsCoordinates.latitude());
                                    media.setLongitude(gpsCoordinates.longitude());


                                    HttpClient client = HttpClient.newHttpClient();
                                    HttpRequest request = HttpRequest.newBuilder()
                                            .uri(URI.create("https://api.3geonames.org/" + gpsCoordinates.latitude() + "," + gpsCoordinates.longitude()))
                                            .build();
                                    client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                                            .thenApply(HttpResponse::body)
                                            .thenAccept( xml -> {
                                                String location = xml.substring(xml.indexOf("<state>") + 7, xml.indexOf("</state>"));
                                                location += ",";
                                                location += xml.substring(xml.indexOf("<city>") + 6, xml.indexOf("</city>"));
                                                media.setLocation(location);
                                            })
                                            .join();

                                }
                                em.persist(media);
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });


            }));
        }
        executorService.shutdown();
        executorService.awaitTermination(3, TimeUnit.HOURS);

        long end = System.currentTimeMillis();
        System.out.println("Converted " + counter + " images to thumbnails. Took " + ((end - start) * 0.001) + "seconds");
    }

    private static boolean isImage(Path path) {
        try {
            String mimeType = Files.probeContentType(path);
            return mimeType != null && mimeType.contains("image");
        } catch (IOException e) {
            return false;
        }
    }


    private static Media.GPSCoordinates gpsCoordinates(Path file) {
        try (InputStream is = Files.newInputStream(file)) {
            Metadata metadata = ImageMetadataReader.readMetadata(is);
            Collection<GpsDirectory> directoriesOfType = metadata.getDirectoriesOfType(GpsDirectory.class);

            if (!directoriesOfType.isEmpty()) {
                GpsDirectory gpsDirectory = directoriesOfType.iterator().next();
                return new Media.GPSCoordinates(gpsDirectory.getGeoLocation().getLatitude(), gpsDirectory.getGeoLocation().getLongitude());
            }
            return null;
        } catch (IOException | ImageProcessingException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static LocalDateTime creationTime(Path file) {
        try {

            try (InputStream is = Files.newInputStream(file)) {
                Metadata metadata = ImageMetadataReader.readMetadata(is);

                Collection<ExifIFD0Directory> directoriesOfType = metadata.getDirectoriesOfType(ExifIFD0Directory.class);

                if (!directoriesOfType.isEmpty()) {
                    ExifIFD0Directory exifIFD0Directory = directoriesOfType.iterator().next();
                    Date creatioDate = exifIFD0Directory.getDate(306);
                    LocalDateTime date = creatioDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime(); // wrong
                    return date;
                }
            }

            BasicFileAttributes attr = Files.readAttributes(file, BasicFileAttributes.class);
            FileTime fileTime = attr.creationTime();
            return LocalDateTime.ofInstant(fileTime.toInstant(), ZoneId.systemDefault());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private boolean createThumbnail(Path image, String hash) {
        try {
            Path thumbnail = getThumbnailPath(hash);
            return imageMagick.createThumbnail(image, thumbnail);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }


    private Path getThumbnailPath(String hash) throws IOException {
        String dir = hash.substring(0, 2);
        String filename = hash.substring(2);

        Path storageDir = thumbnailsDir.resolve(dir);
        if (!Files.exists(storageDir)) {
            Files.createDirectories(storageDir);
        }
        return storageDir.resolve(filename + ".webp");
    }


    public static String hash(Path file) {
        Blake3 hasher = Blake3.newInstance();

        try (InputStream ios = Files.newInputStream(file)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = ios.read(buffer)) != -1) {
                if (read == buffer.length) {
                    hasher.update(buffer);
                } else {
                    hasher.update(Arrays.copyOfRange(buffer, 0, read));
                }
            }
            return hasher.hexdigest();
        } catch (IOException e) {
            e.printStackTrace();
            return "NA";
        }
    }

}
