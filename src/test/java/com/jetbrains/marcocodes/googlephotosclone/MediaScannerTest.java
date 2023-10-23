package com.jetbrains.marcocodes.googlephotosclone;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class MediaScannerTest {

    @Autowired
    private MediaScanner mediaScanner;

    @Test
    void fullscan() {
        mediaScanner.fullscan(Path.of("c:\\tmp"));
    }


}