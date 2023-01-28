package com.jetbrains.marco;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
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
        public record Version(int major, int minor, int patch, Optional<Integer> build) {
            private static final Pattern versionPattern
                    = Pattern.compile("^Version: ImageMagick (\\d+)\\.(\\d+)\\.(\\d+)(?:-(\\d+))?", Pattern.MULTILINE);

            /**
             * Parse the ImageMagick version output into an instance of the `Version` record.
             *
             * @param output Raw ImageMagick version output.
             */
            public static Version fromImageMagickOutput(String output) {
                final var matcher = versionPattern.matcher(output);

                if (!matcher.find() || matcher.groupCount() < 3) {
                    return null;
                }

                final var major = Integer.parseInt(matcher.group(1));
                final var minor = Integer.parseInt(matcher.group(2));
                final var patch = Integer.parseInt(matcher.group(3));

                Optional<Integer> build;

                try {
                    build = Optional.of(Integer.parseInt(matcher.group(4)));
                } catch (NumberFormatException e) {
                    build = Optional.empty();
                }

                return new Version(major, minor, patch, build);
            }
        }

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

                if (version.major == 7) {
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
            final var commands = new String[][]{
                    {"magick", "--version"},
                    {"convert", "--version"}
            };

            for (var command : commands) {
                final var builder = new ProcessBuilder(command);

                try {
                    final var process = builder.start();
                    String versionLine = "";

                    try (final var reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                        String line;

                        while ((line = reader.readLine()) != null) {
                            // We need only one line.
                            if (!line.startsWith("Version")) {
                                continue;
                            }

                            versionLine = line;
                            break;
                        }
                    }

                    if (!process.waitFor(1, TimeUnit.SECONDS) || process.exitValue() != 0) {
                        process.destroy();
                        continue;
                    }

                    final var version = Version.fromImageMagickOutput(versionLine);

                    if (version != null) {
                        return version;
                    }
                } catch (IOException | InterruptedException e) {
                    // e.printStackTrace();
                }
            }

            return null;
        }
    }
}
