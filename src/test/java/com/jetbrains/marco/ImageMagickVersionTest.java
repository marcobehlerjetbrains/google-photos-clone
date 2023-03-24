package com.jetbrains.marco;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

public class ImageMagickVersionTest {

    @ParameterizedTest
    @MethodSource("outputToVersion")
    public void parse_imagemagick_versions(String output, App.ImageMagick.Version expectedVersion) {
        assertThat(App.ImageMagick.Version.of(output)).isEqualTo(expectedVersion);
    }

    private static Stream<Arguments> outputToVersion() {
        return Stream.of(
                arguments("Version: ImageMagick 7.1.0-58 Q16-HDRI x86_64 20802 https://imagemagick.org", App.ImageMagick.Version.IM_7),
                arguments("Version: ImageMagick 6.9.12-73 Q16 x86_64 17646 https://legacy.imagemagick.org", App.ImageMagick.Version.IM_6),
                arguments("Version: ImageMagick 7.1.0 Q16-HDRI x86_64 20802 https://imagemagick.org", App.ImageMagick.Version.IM_7),
                arguments("Version: ImageMagick 7", App.ImageMagick.Version.NA),
                arguments("asdfasdf 1.2.3-5", App.ImageMagick.Version.NA),
                arguments("", App.ImageMagick.Version.NA));
    }
}
