package com.jetbrains.marco;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public class ImageMagick {
    public record Version(int major, int minor, int patch, Optional<Integer> build) {
        private static final Pattern versionPattern
                = Pattern.compile("^Version: ImageMagick (\\d+)\\.(\\d+)\\.(\\d+)(?:-(\\d+))?", Pattern.MULTILINE);

        public static Version fromImageMagickOutput(String output) throws IllegalArgumentException {
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

    // TODO actually read in process output, check for ImageMagick version
    public static Version detectVersion() {
        return new Version(1, 0, 0, Optional.empty());
    }

    public static boolean createThumbnail(Path source, Path target) {
        // magick convert -resize 300x 32.jpg 32_thumb.png
        try {
            System.out.println(Thread.currentThread() + " -> Creating thumbnail: " + target.normalize().toAbsolutePath());
            List<String> resizeThumbnailCommand = new ArrayList<>(List.of("convert", "-resize", "300x"
                    , source.normalize().toAbsolutePath().toString(),
                    target.normalize().toAbsolutePath().toString()));


            if (imageMagickVersion.major == 7) {
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
