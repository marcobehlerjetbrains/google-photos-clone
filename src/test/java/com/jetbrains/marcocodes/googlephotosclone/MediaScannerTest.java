package com.jetbrains.marcocodes.googlephotosclone;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;


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
 *
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class MediaScannerTest {

    @Autowired
    private MediaScanner mediaScanner;

    @Test
    void fullscan() {

        mediaScanner.fullscan(Path.of("c:\\tmp"));

        // print out time
    }


}