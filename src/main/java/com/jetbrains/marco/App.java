package com.jetbrains.marco;

import com.sun.source.tree.Tree;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.github.rctcwyvrn.blake3.Blake3;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

/**
 * Hello world!
 */
public class App {

    static String template = """
            <!DOCTYPE html>
            <html lang="en">
                <head>
                    <meta charset="UTF-8">
                        <title>Title</title>
                    </head>
                <body>
                    <h1>Pictures</h1>
                    {{pics}}
                </body>
            </html>
            """;

    static String userHome = System.getProperty("user.home");
    static Path thumbnailsDir = Path.of(userHome).resolve(".photos");

    static ImageMagick magick = new ImageMagick();
    private static DataSource dataSource = dataSource();


    private static DataSource dataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:h2:file:./media;DB_CLOSE_DELAY=-1;INIT=RUNSCRIPT FROM 'classpath:schema.sql'");
        config.setUsername("marco");
        config.setPassword("marco");
        HikariDataSource ds = new HikariDataSource(config);
        return ds;
    }


    public static void main(String[] args) throws IOException, InterruptedException {
        Files.createDirectories(thumbnailsDir);

        String directory = args.length == 1 ? args[0] : ".";
        Path sourceDir = Path.of(directory);

        AtomicInteger counter = new AtomicInteger();
        long start = System.currentTimeMillis();

        ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        try (Stream<Path> images = Files.walk(sourceDir)
                .filter(Files::isRegularFile)
                .filter(App::isImage);
        ) {
            images.forEach(image -> executorService.submit(() -> {
                String hash = hash(image);
                if (!exists(image, hash)) {
                    final boolean success = createThumbnail(image, hash);
                    if (success) {
                        counter.incrementAndGet();
                        save(image, hash);
                    }
                }
            }));
        }
        executorService.shutdown();
        executorService.awaitTermination(3, TimeUnit.HOURS);

        long end = System.currentTimeMillis();
        System.out.println("Converted " + counter + " images to thumbnails. Took " + ((end - start) * 0.001) + "seconds");

        writeHtmlFile();
    }

    private static void writeHtmlFile() throws IOException {
        Map<LocalDate, List<String>> images = new TreeMap<>();

        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(
                     "select hash, CAST(creation_date AS DATE) creation_date from media " +
                     "order by creation_date desc")) {
            ResultSet resultSet = ps.executeQuery();
            while (resultSet.next()) {
                String hash = resultSet.getString("hash");
                LocalDate creationDate = resultSet.getObject("creation_date", LocalDate.class);

                images.putIfAbsent(creationDate, new ArrayList<>());
                images.get(creationDate).add(hash);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }


        StringBuilder html = new StringBuilder();
        images.forEach((date, hashes) -> {
            html.append("<h2>").append(date).append("</h2>");

            hashes.forEach(hash -> {
                Path image = thumbnailsDir.resolve(hash.substring(0, 2)).resolve(hash.substring(2) + ".webp");
                html.append("<img width='300' src='").append(image.toAbsolutePath()).append("' loading='lazy'/>");
            });
            html.append("<br/>");
        });

        Files.write(Paths.get("./output.html"), template.replace("{{pics}}", html.toString()).getBytes());
    }

    private static boolean exists(Path image, String hash) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement stmt = connection.prepareStatement(
                     "select 1 from media where filename = ? and hash = ?")) {
            stmt.setString(1, image.getFileName().toString());
            stmt.setString(2, hash);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    private static void save(Path image, String hash) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement stmt = connection.prepareStatement(
                     "insert into media (filename, hash, creation_date) values ( ?, ?, ?)")) {
            stmt.setString(1, image.getFileName().toString());
            stmt.setString(2, hash);
            stmt.setObject(3, creationTime(image));
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static LocalDateTime creationTime(Path file) {
        try {
            BasicFileAttributes attr = Files.readAttributes(file, BasicFileAttributes.class);
            FileTime fileTime = attr.creationTime();
            return LocalDateTime.ofInstant(fileTime.toInstant(), ZoneId.systemDefault());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static boolean createThumbnail(Path image, String hash) {
        try {
            Path thumbnail = getThumbnailPath(hash);
            return magick.createThumbnail(image, thumbnail);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }


    private static Path getThumbnailPath(String hash) throws IOException {
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

    private static boolean isImage(Path path) {
        try {
            String mimeType = Files.probeContentType(path);
            return mimeType != null && mimeType.contains("image");
        } catch (IOException e) {
            return false;
        }
    }


    public static class ImageMagick {

        private final Version version = detectVersion();

        public int run(String... cmds) throws IOException, InterruptedException {
            ProcessBuilder builder = new ProcessBuilder(cmds);
            builder.inheritIO();
            Process process = builder.start();
            boolean finished = process.waitFor(1, TimeUnit.SECONDS);
            if (!finished) {
                process.destroy();
            }
            return process.exitValue();
        }

        public boolean createThumbnail(Path source, Path target) {
            // magick convert -resize 300x 32.jpg 32_thumb.png
            try {
                System.out.println("Creating thumbnail: " + target.normalize().toAbsolutePath());
                List<String> cmd = new ArrayList<>(List.of("convert", "-resize", "300x"
                        , source.normalize().toAbsolutePath().toString(),
                        target.normalize().toAbsolutePath().toString()));

                if (version == Version.IM_7) {
                    cmd.add(0, "magick");
                }
                ProcessBuilder builder = new ProcessBuilder(cmd);
                builder.inheritIO();
                Process process = builder.start();
                boolean finished = process.waitFor(5, TimeUnit.SECONDS);
                if (!finished) {
                    process.destroy();
                }
                return finished & process.exitValue() == 0;
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
                return false;
            }
        }

        public Version detectVersion() {
            try {
                int exitCode = run("magick", "--version");
                if (exitCode == 0) {
                    return Version.IM_7;
                }

                exitCode = run("convert", "--version");
                if (exitCode == 0) {
                    return Version.IM_6;
                }
                return Version.NA;
            } catch (Exception e) {
                return Version.NA;
            }
        }

        public enum Version {
            NA, IM_6, IM_7
        }
    }
}
