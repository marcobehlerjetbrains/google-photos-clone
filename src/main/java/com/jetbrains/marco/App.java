package com.jetbrains.marco;

import com.jetbrains.marco.tables.Media;
import io.github.rctcwyvrn.blake3.Blake3;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static com.jetbrains.marco.tables.Media.MEDIA;

/**
 * Hello world!
 */
public class App {

    static String userHome = System.getProperty("user.home");
    static Path thumbnailsDir = Path.of(userHome).resolve(".photos");

    public static void main(String[] args) throws IOException {
        if (ImageMagick.imageMagickVersion == null) {
            System.err.println("Sorry, you don't have ImageMagick installed or it's not on your PATH, I'm helpless, I don't know what to do now."); // bonus points for proper instructions...
            System.exit(1);
        }


        Files.createDirectories(thumbnailsDir);

        String directory = args.length == 1 ? args[0] : ".";
        Path sourceDir = Path.of(directory);

        AtomicInteger counter = new AtomicInteger();
        long start = System.currentTimeMillis();

        ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() - 1);


        try (Stream<Path> files = Files.walk(sourceDir)
                .filter(Files::isRegularFile)
                .filter(App::isImage)) {
            files.forEach(f -> {
                executorService.submit(() -> {
                    String hash = createHash(f);
                    Path dir = thumbnailsDir.resolve(hash.substring(0, 2));
                    if (!Files.exists(dir)) {
                        try {
                            Files.createDirectories(dir);
                        } catch (IOException e) {
                        }
                    }
                    String filename = hash.substring(2);

                    Path targetFilename = dir.resolve(filename + ".jpeg");

                    boolean thumbnailCreated = new ImageMagick().createThumbnail(f, targetFilename);

                    if (thumbnailCreated) {
                        counter.incrementAndGet();
                        createMetadataFile(f, dir.resolve(filename + ".txt"));
                        saveToDatabase(f, targetFilename);
                    }
                });
            });
        }

        try {
            executorService.shutdown();
            executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        long end = System.currentTimeMillis();
        System.out.println("Converted " + counter + " images to thumbnails. Took " + ((end - start) * 0.001) + "seconds");
    }

    private static void saveToDatabase(Path f, Path targetFilename) {
        try (Connection conn = DriverManager.getConnection("jdbc:h2:~/media;AUTO_SERVER=TRUE")) {
            DSLContext create = DSL.using(conn, SQLDialect.H2);

            int execute = create.insertInto(MEDIA, MEDIA.FILE_NAME, MEDIA.REFERENCE)
                    .values(f.getFileName().toString(), targetFilename.getFileName().toString())
                    .execute();
            System.out.println("execute = " + execute);


        /*    Result<Record> records = create.select().from(MEDIA).fetch();

            for (Record r : records) {
                Integer id = r.getValue(MEDIA.ID);
                String filename = r.getValue(MEDIA.FILE_NAME);

                System.out.println("ID: " + id + " FILENAME: " + filename);
            }
*/
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }


    }

    private static void createMetadataFile(Path f, Path resolve) {
        // TODO replace
        try {
            BasicFileAttributes basicFileAttributes = Files.getFileAttributeView(f, BasicFileAttributeView.class).readAttributes();
            FileTime fileTime = basicFileAttributes.creationTime();
            Files.write(resolve, fileTime.toString().getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }





    private static String createHash(Path f) {
        Blake3 hasher = Blake3.newInstance();

        try (InputStream ios = Files.newInputStream(f)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = ios.read(buffer)) != -1) {
                if (read == buffer.length) {
                    hasher.update(buffer);
                } else {
                    hasher.update(Arrays.copyOfRange(buffer, 0, read));
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return hasher.hexdigest();
    }

    private static boolean isImage(Path path) {
        try {
            String mimeType = Files.probeContentType(path);
            return mimeType != null && mimeType.contains("image");
        } catch (IOException e) {
            return false;
        }
    }


}


