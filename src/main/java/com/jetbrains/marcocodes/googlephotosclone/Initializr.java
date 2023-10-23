package com.jetbrains.marcocodes.googlephotosclone;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class Initializr implements ApplicationRunner {

    private final Logger logger = LoggerFactory.getLogger(Initializr.class);
    private final MediaScanner mediaScanner;

    public Initializr(MediaScanner mediaScanner) {
        this.mediaScanner = mediaScanner;
    }


    @Override
    public void run(ApplicationArguments args) throws Exception {
        String userHome = System.getProperty("user.home");
        Path thumbnailsDir = Path.of(userHome).resolve(".photos");
        Files.createDirectories(thumbnailsDir);

        String directory = args.getSourceArgs().length == 1 ? args.getSourceArgs()[0] : ".";
        Path sourceDir = Path.of(directory);

        long start = System.currentTimeMillis();
        int scanned = mediaScanner.fullscan(sourceDir);
        long end = System.currentTimeMillis();

        logger.info("Converted " + scanned + " images to thumbnails. Took " + ((end - start) * 0.001) + "seconds");
    }

}
