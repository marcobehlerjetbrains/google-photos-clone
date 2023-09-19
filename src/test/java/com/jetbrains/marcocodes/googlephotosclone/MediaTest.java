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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class MediaTest {

    @TestFactory
    Collection<DynamicTest> dynamicTestsCreated() {
        List<String> result = new ArrayList<>();

        try (ScanResult scanResult = new ClassGraph().scan()) {
            scanResult
                    .getResourcesMatchingPattern(Pattern.compile("image-(.*)\\.jpg"))
                    .forEach((resource) -> {
                        result.add(resource.getPath());
                    });
        }

        return result.stream().map(image -> DynamicTest.dynamicTest(image, () -> {
            String filename = image.substring(0, image.indexOf("."));

            try (InputStream json = MediaTest.class.getResourceAsStream("/" + filename + ".json");
                 InputStream imageStream = MediaTest.class.getResourceAsStream("/" + image)) {

                TestMetadata testMetadata = new ObjectMapper().registerModule(new JavaTimeModule()).readValue(json, TestMetadata.class);
                Metadata metadata = ImageMetadataReader.readMetadata(imageStream);

                Initializr.Dimensions dimensions = Initializr.getImageSize(metadata);
                assertThat(dimensions.height()).isEqualTo(testMetadata.height());
                assertThat(dimensions.width()).isEqualTo(testMetadata.width());

                LocalDateTime creationTime = Initializr.getCreationTime(Path.of(MediaTest.class.getResource("/" + image).getFile().substring(1)), metadata);
                assertThat(creationTime.truncatedTo(ChronoUnit.MINUTES)).isEqualToIgnoringSeconds(testMetadata.date());

                Location location = Initializr.getLocation(metadata);

                if (testMetadata.longitude() == null || testMetadata.latitude() == null) {
                    assertThat(location).isNull();
                }
                else {
                    assertThat(location.getDms()).isEqualTo(testMetadata.latitude() + ", " + testMetadata.longitude());
                }


            }
        })).collect(Collectors.toList());

    }


}
