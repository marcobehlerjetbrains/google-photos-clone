package com.jetbrains.marco;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;


class ImageMagickTest {

    public App.ImageMagick imageMagick = new App.ImageMagick();

    @Test
    void imageMagick_is_installed() {
        assertThat(new App.ImageMagick().detectVersion()).isNotEqualTo(App.ImageMagick.Version.NA);
    }

    @Test
    @EnabledIfImageMagickIsInstalled
    void thumbnail_creation_works(@TempDir Path testDir) throws IOException {
        Path originalImage = copyTestImageTo(testDir.resolve("large.jpg"));
        Path thumbnail = testDir.resolve("thumbnail.jpg");

        imageMagick.createThumbnail(originalImage, thumbnail);

        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(thumbnail).exists();
        softly.assertThat(Files.size(thumbnail)).isLessThan(Files.size(originalImage)/ 2);
        softly.assertThat(getDimensions(thumbnail).width()).isEqualTo(300);
        softly.assertAll();

        // ??? imagemagick...
    }

    @ParameterizedTest
    @MethodSource("stringToVersion")
    public void parse_imagemagick_versions(String output, App.ImageMagick.Version expectedVersion) {
        assertThat(App.ImageMagick.Version.of(output)).isEqualTo(expectedVersion);
    }

    private static Stream<Arguments> stringToVersion() {
        return Stream.of(
                arguments("Version: ImageMagick 7.1.0-58 Q16-HDRI x86_64 20802 https://imagemagick.org", App.ImageMagick.Version.IM_7),
                arguments("Version: ImageMagick 6.9.12-73 Q16 x86_64 17646 https://legacy.imagemagick.org", App.ImageMagick.Version.IM_6),
                arguments("Version: ImageMagick 7.1.0 Q16-HDRI x86_64 20802 https://imagemagick.org", App.ImageMagick.Version.IM_7),
                arguments("Version: ImageMagick 7", App.ImageMagick.Version.NA),
                arguments("asdfasdf 1.2.3-5", App.ImageMagick.Version.NA),
                arguments("", App.ImageMagick.Version.NA));
    }

    private Dimensions getDimensions(Path path) {
        try (InputStream is = Files.newInputStream(path)) {
            BufferedImage read = ImageIO.read(is);
            return new Dimensions(read.getWidth(), read.getHeight());
        } catch (IOException e) {
            return new Dimensions(-1, -1);
        }
    }

    public record Dimensions(int width, int height) {

    }


    private static Path copyTestImageTo(Path targetFile) {
        try (InputStream resourceAsStream = ImageMagickTest.class.getResourceAsStream("/pexels-pixabay-47367.jpg")) {
            Files.copy(resourceAsStream, targetFile, StandardCopyOption.REPLACE_EXISTING);
            System.out.println("Copied test image to: = " + targetFile.toAbsolutePath());
            return targetFile;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

}