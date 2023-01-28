package com.jetbrains.marco;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public class ImageMagick {
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

    public static final Version imageMagickVersion = detectVersion();

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

    public static Version detectVersion() {
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

    public static boolean createThumbnail(Path source, Path target) {
        // magick convert -resize 300x 32.jpg 32_thumb.png
        try {
            System.out.println(Thread.currentThread() + " -> Creating thumbnail: " + target.normalize().toAbsolutePath());

            List<String> resizeThumbnailCommand = new ArrayList<>(
                    List.of("convert", "-resize", "300x", source.normalize().toAbsolutePath().toString(),
                    target.normalize().toAbsolutePath().toString())
            );

            if (Objects.requireNonNull(imageMagickVersion).major == 7) {
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
