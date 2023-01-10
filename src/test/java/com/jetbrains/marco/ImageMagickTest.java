package com.jetbrains.marco;

import org.assertj.core.api.JUnitJupiterSoftAssertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.contentOf;

class ImageMagickTest {

    @RegisterExtension
    public final JUnitJupiterSoftAssertions softly = new JUnitJupiterSoftAssertions();

    private ImageMagick imageMagick = new ImageMagick();

    @Test
    public void imageMagick_isInstalled() {
        assertThat(
                ImageMagick.detectVersion()).as("No ImageMagick installed or not on your PATH. See %URL% for installation instructions.")
                .isNotEqualTo(ImageMagick.ImageMagickVersion.NA);
    }

    @Test
    @EnabledIfImageMagickInstalled
    public void imageMagick_creates_proper_thumbnails(@TempDir Path tempDir) throws IOException {

        Path testImage = copyTestImageTo(tempDir.resolve("large.jpg"));
        Path thumbnail = tempDir.resolve("thumbnail.jpg");

        imageMagick.createThumbnail(testImage, thumbnail);

        softly.assertThat(thumbnail).exists();
        softly.assertThat(thumbnail).isNotEmptyFile();
        softly.assertThat(getImageDimensions(thumbnail).width()).isEqualTo(300);
        softly.assertThat(Files.size(thumbnail)).isLessThan((long) (Files.size(testImage) * 0.5)); // thumb should be considerably smaller than original, 50% is just a guess
    }



    public record Dimensions(int width, int height) {}


    public Dimensions getImageDimensions(Path path) {
        try (InputStream is = Files.newInputStream(path)) {
            BufferedImage read = ImageIO.read(is);
            return new Dimensions(read.getWidth(), read.getHeight());
        } catch (IOException e) {
            return new Dimensions(-1, -1);
        }
    }


    private static Path copyTestImageTo(Path targetFile) {
        try (InputStream resourceAsStream = ImageMagickTest.class.getResourceAsStream("/large.jpg")) {
            Files.copy(resourceAsStream, targetFile, StandardCopyOption.REPLACE_EXISTING);
            System.out.println("test.toAbsolutePath() = " + targetFile.toAbsolutePath());
            return targetFile;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
}