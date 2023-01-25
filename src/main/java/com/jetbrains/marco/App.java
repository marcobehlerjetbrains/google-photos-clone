package com.jetbrains.marco;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
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
                new ImageMagick().createThumbnail(f, thumbnailsDir.resolve(f.getFileName()));
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


    public static class ImageMagick {

        private Version version = detectVersion();

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
