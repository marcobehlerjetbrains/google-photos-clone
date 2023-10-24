package com.jetbrains.marcocodes.googlephotosclone;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import java.nio.file.Path;


/**
 *
 *
 gradle test --tests org.gradle.SomeTest.someSpecificFeature
 gradle test --tests '*SomeTest.someSpecificFeature'
 gradle test --tests '*SomeSpecificTest'
 gradle test --tests 'all.in.specific.package*'
 gradle test --tests '*IntegTest'
 gradle test --tests '*IntegTest*ui*'
 gradle test --tests '*IntegTest.singleMethod'
 gradle someTestTask --tests '*UiTest' someOtherTestTask --tests '*WebTest*ui'


 :test --tests "com.jetbrains.marcocodes.googlephotosclone.MediaScannerTest" -Pperformance.test=true

 *
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@EnabledIfSystemProperty(named = "performance.test", matches = "true")
class MediaScannerTest {

    @Value("${scan.dir}")
    private String scanDirectory;

    @Autowired
    private MediaScanner mediaScanner;

    @Test
    void fullscan() {
        mediaScanner.fullscan(Path.of(this.scanDirectory));

       // mediaScanner.fullscan(Path.of("c:\\tmp"));

        // print out time
    }


}