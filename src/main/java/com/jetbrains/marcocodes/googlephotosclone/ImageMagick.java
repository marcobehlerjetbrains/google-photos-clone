package com.jetbrains.marcocodes.googlephotosclone;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
public class ImageMagick {

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
            String resizeDimensions = "x217";
            System.out.println("Creating thumbnail: " + target.normalize().toAbsolutePath());
            List<String> cmd = new ArrayList<>(List.of("convert", "-resize", resizeDimensions
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
