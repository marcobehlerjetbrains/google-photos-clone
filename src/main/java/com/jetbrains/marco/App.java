package com.jetbrains.marco;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

/**
 * Hello world!
 */
public class App {

    static String userHome = System.getProperty("user.home");
    static Path thumbnailsDir = Path.of(userHome).resolve(".photos");

    public static void main(String[] args) throws IOException {
        Files.createDirectories(thumbnailsDir);

        String directory = args.length == 1 ? args[0] : ".";
        Path sourceDir = Path.of(directory);

        AtomicInteger counter = new AtomicInteger();
        long start = System.currentTimeMillis();

        try (Stream<Path> files = Files.walk(sourceDir)
                .filter(Files::isRegularFile)
                .filter(App::isImage)) {
            files.forEach(f -> {
                counter.incrementAndGet();
                createThumbnail(f, thumbnailsDir.resolve(f.getFileName()));
            });
        }

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

    private static boolean createThumbnail(Path source, Path target) {
        // magick convert -resize 300x 32.jpg 32_thumb.png
        try {
            System.out.println("Creating thumbnail: " + target.normalize().toAbsolutePath());
            List<String> cmd = List.of("magick", "convert", "-resize", "300x"
                    , source.normalize().toAbsolutePath().toString(),
                    target.normalize().toAbsolutePath().toString());
            ProcessBuilder builder = new ProcessBuilder(cmd);
            builder.inheritIO();
            Process process = builder.start();
            boolean finished = process.waitFor(3, TimeUnit.SECONDS);
            if (!finished) {
                process.destroy();
            }
            return finished;
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return false;
        }
    }
}
