package com.jetbrains.marco;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

public class ImageMagickVersionTest {
    /**
     * Wraps test version output string and the expected parse results.
     *
     * @param output          Version line as returned by magick --version or convert --version commands.
     * @param expectedVersion Expected version data.
     */
    private record VersionInfoTestData(String output, ImageMagick.Version expectedVersion) {
    }

    @ParameterizedTest
    @MethodSource("provideVersionInfoTestData")
    public void can_parse_ImageMagick_output(VersionInfoTestData versionInfo) {
        final var version = ImageMagick.Version.fromImageMagickOutput(versionInfo.output);

        assertThat(version).isNotNull();
        assertThat(version.major()).isEqualTo(versionInfo.expectedVersion.major());
        assertThat(version.minor()).isEqualTo(versionInfo.expectedVersion.minor());
        assertThat(version.patch()).isEqualTo(versionInfo.expectedVersion.patch());

        if (versionInfo.expectedVersion.build().isPresent()) {
            assertThat(version.build().isPresent()).isTrue();
            assertThat(version.build().get()).isEqualTo(versionInfo.expectedVersion.build().get());
        } else {
            assertThat(version.build().isEmpty()).isTrue();
        }
    }

    @ParameterizedTest
    @MethodSource("provideInvalidVersionOutputs")
    public void fromImageMagickOutput_returns_null_on_invalid_version_outputs(String versionOutput) {
        assertThat(ImageMagick.Version.fromImageMagickOutput(versionOutput)).isNull();
    }

    private static VersionInfoTestData[] provideVersionInfoTestData() {
        return new VersionInfoTestData[]{
                // ImageMagick 7
                new VersionInfoTestData("Version: ImageMagick 7.1.0-58 Q16-HDRI x86_64 20802 https://imagemagick.org",
                        new ImageMagick.Version(7, 1, 0, Optional.of(58))),
                // ImageMagick 6
                new VersionInfoTestData("Version: ImageMagick 6.9.12-73 Q16 x86_64 17646 https://legacy.imagemagick.org",
                        new ImageMagick.Version(6, 9, 12, Optional.of(73))),
                // ImageMagick without build data
                new VersionInfoTestData("Version: ImageMagick 7.1.0 Q16-HDRI x86_64 20802 https://imagemagick.org",
                        new ImageMagick.Version(7, 1, 0, Optional.empty())),
        };
    }

    private static String[] provideInvalidVersionOutputs() {
        return new String[]{
                "Version: ImageMagick 7.1-24",
                "Version: ImageMagick 7.1",
                "Version: ImageMagick 7",
                "asdfasdf 1.2.3-5",
                "asdfasdf 1.2.3",
                "1.2.3-5",
                "1.2.3",
                ""
        };
    }
}
