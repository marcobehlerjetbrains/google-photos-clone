package com.jetbrains.marco;

import io.github.rctcwyvrn.blake3.Blake3;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
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

    static ImageMagickVersion imageMagickVersion;


    public enum ImageMagickVersion {
        NA, IM6, IM7
    }

    // TODO actually read in process output, check for ImageMagick version
    public static ImageMagickVersion detectImageMagickVersion() {
        try {
            int exitValue = runCommand(List.of("magick", "--version"));
            if (exitValue == 0) {
                return ImageMagickVersion.IM7;
            }


            exitValue = runCommand(List.of("convert", "--version"));
            if (exitValue == 0) {
                return ImageMagickVersion.IM6;
            }

            return ImageMagickVersion.NA;
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return ImageMagickVersion.NA;
        }
    }

    private static int runCommand(List<String> cmd) throws IOException, InterruptedException {
        ProcessBuilder builder = new ProcessBuilder(cmd);
        builder.inheritIO();
        Process process = builder.start();
        boolean finished = process.waitFor(1, TimeUnit.SECONDS);  // not really correct...how to improve?
        if (!finished) {
            process.destroy();
        }

        int exitValue = process.exitValue();
        return exitValue;
    }

    public static void main(String[] args) throws IOException {
        imageMagickVersion = detectImageMagickVersion();
        System.out.println("Detected ImageMagick Version: " + imageMagickVersion);
        if (imageMagickVersion == ImageMagickVersion.NA) {
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
                    if (!Files.exists(dir)) { try { Files.createDirectories(dir); } catch (IOException e) {} }
                    String filename = hash.substring(2);

                    boolean thumbnailCreated = createThumbnail(f, dir.resolve(filename + ".jpeg"));

                    if (thumbnailCreated) {
                        counter.incrementAndGet();
                        createMetadataFile(f, dir.resolve(filename + ".txt"));
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

    private static boolean createThumbnail(Path source, Path target) {
        // magick convert -resize 300x 32.jpg 32_thumb.png
        try {
            System.out.println(Thread.currentThread() + " -> Creating thumbnail: " + target.normalize().toAbsolutePath());
            List<String> resizeThumbnailCommand = new ArrayList<>(List.of("convert", "-resize", "300x"
                    , source.normalize().toAbsolutePath().toString(),
                    target.normalize().toAbsolutePath().toString()));


            if (imageMagickVersion == ImageMagickVersion.IM7) {
                resizeThumbnailCommand.add(0, "magick");
            }

            ProcessBuilder builder = new ProcessBuilder(resizeThumbnailCommand);
            builder.inheritIO();
            Process process = builder.start();
            boolean finished = process.waitFor(3, TimeUnit.SECONDS);
            if (!finished) {
                process.destroy();
            }
            return finished && process.exitValue() == 0;
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return false;
        }
    }
}


