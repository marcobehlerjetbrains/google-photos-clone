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


 ./gradlew cleanTest :test --tests "com.jetbrains.marcocodes.googlephotosclone.MediaScannerTest" -Pperformance.test=false -Pstartup.fullscan=false -Pscan.dir=C:\\tmp

 :test --tests "com.jetbrains.marcocodes.googlephotosclone.MediaScannerTest" -Pperformance.test=true


 ./gradlew cleanTest :test --tests "com.jetbrains.marcocodes.googlephotosclone.MediaScannerTest" -Pperformance.test=false -Pstartup.fullscan=false -Pscan.dir=/srv/syncthing/marco -P spring.datasource.url=jdbc:h2:../db;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=false



 ./gradlew cleanTest :test --tests "com.jetbrains.marcocodes.googlephotosclone.MediaScannerTest" -Pperformance.test=true -Pstartup.fullscan=false -Pscan.dir=/srv/syncthing/marco -P spring.datasource.url=jdbc:h2:../db;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=false

 spring.datasource.url=jdbc:h2:./db;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=false

 *
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@EnabledIfSystemProperty(named = "performance.test", matches = "true")
class ManualPerformanceTest {

    @Value("${scan.dir}")
    private String scanDirectory;

    @Autowired
    private MediaScanner mediaScanner;

    @Test
    void fullscan() {
        mediaScanner.fullscan(Path.of(this.scanDirectory));
    }

    @Test
    void fullscanNewAlgo() {
        mediaScanner.fullscanNewAlgo(Path.of(this.scanDirectory));
    }


}