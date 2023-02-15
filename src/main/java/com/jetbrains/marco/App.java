package com.jetbrains.marco;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
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

        public ImageMagick.Version detectVersion() {
            try {
                ProcessResult result = run("magick", "--version");
                if (result.exitValue == 0) {
                    return ImageMagick.Version.of(result.output);
                }

                result = run("convert", "--version");
                if (result.exitValue == 0) {
                    return ImageMagick.Version.of(result.output);
                }

                return ImageMagick.Version.NA;
            } catch (Exception e) {
                return ImageMagick.Version.NA;
            }
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



        public ProcessResult run(String... cmds) throws IOException, InterruptedException {
            ProcessBuilder builder = new ProcessBuilder(cmds)
                    .redirectOutput(ProcessBuilder.Redirect.PIPE)
                    .redirectInput(ProcessBuilder.Redirect.INHERIT)
                    .redirectError(ProcessBuilder.Redirect.INHERIT);

            Process process = builder.start();
            StringBuilder output = new StringBuilder();

            try (final var reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line);
                }
            }
            boolean finished = process.waitFor(1, TimeUnit.SECONDS);
            if (!finished) {
                process.destroy();
            }
            return new ProcessResult(process.exitValue(), output.toString());
        }




        public record ProcessResult(int exitValue, String output) {

        }


        public enum Version {
            NA, IM_6, IM_7;


            public static Version of(String string) {
                if (string == null || string.isBlank()) {
                    return Version.NA;
                }

                if (string.startsWith("Version: ImageMagick 7.")) {
                    return Version.IM_7;
                }

                if (string.startsWith("Version: ImageMagick 6.")) {
                    return Version.IM_6;
                }
                return Version.NA;
            }


        }
    }
}
