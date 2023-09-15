package com.jetbrains.marcocodes.googlephotosclone;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.png.PngChunkType;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.exif.GpsDirectory;
import com.drew.metadata.jpeg.JpegDirectory;
import com.drew.metadata.png.PngDirectory;
import com.google.common.collect.Iterators;
import io.github.rctcwyvrn.blake3.Blake3;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Tuple;
import org.hibernate.SessionFactory;
import org.hibernate.query.NativeQuery;
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
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class Initializr implements ApplicationRunner {

    static String userHome = System.getProperty("user.home");
    static Path thumbnailsDir = Path.of(userHome).resolve(".photos");
    private final Queries queries_;
    private final ImageMagick imageMagick;

    private final EntityManagerFactory emf;

    private final ExecutorService executorService;

    public Initializr(Queries queries_, ImageMagick imageMagick, EntityManagerFactory emf, ExecutorService executorService) {
        this.queries_ = queries_;
        this.imageMagick = imageMagick;
        this.emf = emf;
        this.executorService = executorService;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        Files.createDirectories(thumbnailsDir);

        String directory = args.getSourceArgs().length == 1 ? args.getSourceArgs()[0] : ".";
        Path sourceDir = Path.of(directory);

        AtomicInteger counter = new AtomicInteger();
        long start = System.currentTimeMillis();

        try (Stream<Path> images = Files.walk(sourceDir)
                .parallel()
                .filter(Files::isRegularFile)
                .filter(Initializr::isImage)
        ) {
            Iterators.partition(images.iterator(), 50).forEachRemaining(batch -> {
                executorService.submit(() -> {
                    emf.unwrap(SessionFactory.class).inStatelessTransaction(statelessSession -> {
                        Map<Path, String> filesToHashes = batch.stream().collect(Collectors.toMap(f -> f, Initializr::hash));

                        StringBuilder builder = new StringBuilder();
                        Iterator<Map.Entry<Path, String>> iterator = filesToHashes.entrySet().iterator();
                        while (iterator.hasNext()) {
                            Map.Entry<Path, String> entry = iterator.next();
                            builder.append("('").append(entry.getKey().getFileName().toString()).append("','").append(entry.getValue()).append("')");
                            if (iterator.hasNext()) builder.append(",");
                        }

                        String ququ = "WITH media_batch AS (\n" +
                                      "    SELECT filename, hash\n" +
                                      "    FROM (\n" +
                                      "        VALUES " + builder.toString() + ")" +
                                      "        v(filename,hash)\n" +
                                      ")" +
                                      "select filename, hash from media_batch" +
                                      " where not exists(" +
                                      "    select * from MEDIA m where m.filename = media_batch.filename and m.hash = media_batch.hash" +
                                      ")" +
                                      "\n";
                        NativeQuery<Tuple> nativeQuery = statelessSession.createNativeQuery(ququ, Tuple.class);
                        Map<String, String> collect = nativeQuery.getResultList().stream().collect(Collectors.toMap(non -> non.get(Media_.FILENAME, String.class), non -> non.get(Media_.HASH, String.class)));

                        filesToHashes
                                .entrySet()
                                .stream()
                                .filter(entry -> {
                                    Path fileName = entry.getKey().getFileName();
                                    return collect.containsKey(fileName.toString()) && collect.get(fileName.toString()).equals(entry.getValue());
                                })
                                .forEach((entry) -> {
                                    var image = entry.getKey();
                                    var hash = entry.getValue();
                                    try (InputStream is = Files.newInputStream(image)) {
                                        Metadata metadata = ImageMetadataReader.readMetadata(is);
                                        Dimensions dimensions = getImageSize(image, metadata);
                                        Location location = getLocation(image, metadata);
                                        LocalDateTime creationTime = creationTime(image, metadata);

                                        final boolean success = createThumbnail(image, hash, dimensions);
                                        if (success) {
                                            counter.incrementAndGet();
                                            Media media = new Media(hash, image.getFileName().toString(), creationTime, location);
                                            statelessSession.insert(media);
                                        }
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                });

                    });
                });
            });
            /*images.forEach(image -> emf.unwrap(SessionFactory.class)
                    .inStatelessSession(session -> session.insert(new MediaTask(image.toAbsolutePath().normalize().toString()))));*/
        }


        long end = System.currentTimeMillis();
        System.out.println("Scanned " + counter + " images to process. Took " + ((end - start) * 0.001) + "seconds");
    }


    private static boolean isImage(Path path) {
        try {
            String mimeType = Files.probeContentType(path);
            return mimeType != null && mimeType.contains("image");
        } catch (IOException e) {
            return false;
        }
    }

    private static Dimensions getImageSize(Path image, Metadata metadata) {
        Iterable<Directory> directories = metadata.getDirectories();
        for (Directory d : directories) {
            try {
                if (d instanceof PngDirectory && ((PngDirectory) d).getPngChunkType().equals(PngChunkType.IHDR)) {
                    int width = d.getInt(1);
                    int height = d.getInt(2);
                    return new Dimensions(width, height);
                } else if (d instanceof ExifIFD0Directory && d.containsTag(256) && d.containsTag(257)) {
                    int width = d.getInt(256);
                    int height = d.getInt(257);
                    return new Dimensions(width, height);
                } else if (d instanceof JpegDirectory && d.containsTag(3) && d.containsTag(1)) {
                    int width = d.getInt(3);
                    int height = d.getInt(1);
                    return new Dimensions(width, height);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        System.err.println("I unfortunately don't understand the image YET: " + image.toAbsolutePath());
        return new Dimensions(0, 0);
    }

    private static LocalDateTime creationTime(Path file, Metadata metadata) {
        ExifIFD0Directory exifIFD0Directory = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);

        if (exifIFD0Directory != null) {
            Date creatioDate = exifIFD0Directory.getDate(306);
            LocalDateTime date = creatioDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime(); // wrong
            return date;
        }
        try {
            BasicFileAttributes attr = Files.readAttributes(file, BasicFileAttributes.class);
            FileTime fileTime = attr.creationTime();
            return LocalDateTime.ofInstant(fileTime.toInstant(), ZoneId.systemDefault());
        } catch (IOException e) {
            e.printStackTrace();
        }

        return LocalDateTime.now();
    }

    private boolean createThumbnail(Path image, String hash, Dimensions dimensions) {
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


    private static Location getLocation(Path file, Metadata metadata) {
        Collection<GpsDirectory> directoriesOfType = metadata.getDirectoriesOfType(GpsDirectory.class);

        if (!directoriesOfType.isEmpty()) {
            StringBuilder result = new StringBuilder();
            GpsDirectory gpsDirectory = directoriesOfType.iterator().next();
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.3geonames.org/" + gpsDirectory.getGeoLocation().getLatitude() + "," + gpsDirectory.getGeoLocation().getLongitude()))
                    .build();
            client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply(HttpResponse::body)
                    .thenAccept(xml -> {
                        String location = xml.substring(xml.indexOf("<state>") + 7, xml.indexOf("</state>"));
                        location += ",";
                        location += xml.substring(xml.indexOf("<city>") + 6, xml.indexOf("</city>"));
                        result.append(location);
                    })
                    .join();

            return new Location(gpsDirectory.getGeoLocation().getLatitude(), gpsDirectory.getGeoLocation().getLongitude(), result.toString());
        }
        return null;
    }




    record Dimensions(int width, int height) {
    }


}
