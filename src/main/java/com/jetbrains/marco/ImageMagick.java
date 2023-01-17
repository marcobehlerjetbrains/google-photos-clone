package com.jetbrains.marco;

import com.jetbrains.marco.tables.records.MediaRecord;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class ImageMagick {

    static ImageMagickVersion imageMagickVersion;


    public enum ImageMagickVersion {
        NA, IM6, IM7
    }



    static {
        imageMagickVersion = detectVersion();
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



    // TODO actually read in process output, check for ImageMagick version
    public static ImageMagickVersion detectVersion() {
        try {
            System.out.println("Detected ImageMagick Version: " + imageMagickVersion);
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

    public static boolean createThumbnail(Path source, Path target) {
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
