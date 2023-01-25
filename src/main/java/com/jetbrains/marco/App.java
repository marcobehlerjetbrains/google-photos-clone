package com.jetbrains.marco;

import io.github.rctcwyvrn.blake3.Blake3;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

/**
 * Hello world!
 */
public class App {

    static String userHome = System.getProperty("user.home");
    static Path thumbnailsDir = Path.of(userHome).resolve(".photos");

    static ImageMagick magick = new ImageMagick();

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
                final boolean success = createThumbnail(image);
                if (success) counter.incrementAndGet();
            }));
        }
        executorService.shutdown();
        executorService.awaitTermination(3, TimeUnit.HOURS);

        long end = System.currentTimeMillis();
        System.out.println("Converted " + counter + " images to thumbnails. Took " + ((end - start) * 0.001) + "seconds");
    }

    private static boolean createThumbnail(Path image) {
        try {
            Path thumbnail = getThumbnailPath(image);
            return magick.createThumbnail(image, thumbnail);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private static Path getThumbnailPath(Path image) throws IOException {
        String hash = hash(image);
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
