package com.jetbrains.marcocodes.googlephotosclone;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.io.InputStream;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

public class MediaTest {

    ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @TestFactory
    Collection<DynamicTest> mediaTests() {
        try (ScanResult scanResult = new ClassGraph().scan()) {
            List<String> images = scanResult
                    .getResourcesMatchingPattern(Pattern.compile("image-(.*)\\.jpg"))
                    .getPaths();

            return images.stream().map(image -> DynamicTest.dynamicTest(image, () -> {
                String filename = image.substring(0, image.indexOf("."));

                try (InputStream json = MediaTest.class.getResourceAsStream("/" + filename + ".json");) {
                    TestMetadata expectedMetadata = objectMapper.readValue(json, TestMetadata.class);

                    Path file = Path.of(MediaTest.class.getResource("/" + image).toURI());
                    Metadata actualMetadata = ImageMetadataReader.readMetadata(file.toFile());

                    // 1. dimensions
                    Initializr.Dimensions actualDimensions = Initializr.getImageSize(actualMetadata);
                    assertThat(actualDimensions.height()).as("image height").isEqualTo(expectedMetadata.height());
                    assertThat(actualDimensions.width()).as("image width").isEqualTo(expectedMetadata.width());

                    // 2. creation time
                    LocalDateTime creationTime = Initializr.getCreationTime(file, actualMetadata);
                    assertThat(creationTime).as("creation time").isEqualToIgnoringSeconds(expectedMetadata.date());

                    // 3. location
                    Location actualLocation = Initializr.getLocation(actualMetadata);
                    if (expectedMetadata.longitude() == null || expectedMetadata.latitude() == null) {
                        assertThat(actualLocation).isNull();
                    } else {
                        assertThat(actualLocation).isNotNull();
                        assertThat(actualLocation.getDms()).isEqualTo(expectedMetadata.latitude() + ", " + expectedMetadata.longitude());
                    }
                }
            })).toList();
        }


    }


}
