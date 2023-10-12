package com.jetbrains.marcocodes.googlephotosclone;

import jakarta.persistence.EntityManagerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.http.HttpClient;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

@Component
public class Initializr implements ApplicationRunner {

    static String userHome = System.getProperty("user.home");
    static Path thumbnailsDir = Path.of(userHome).resolve(".photos");
    private final Queries queries_;
    private final ImageMagick imageMagick;

    private final EntityManagerFactory emf;

    static HttpClient client = HttpClient.newHttpClient();
    private final JmsTemplate jmsTemplate;

    public Initializr(Queries queries_, ImageMagick imageMagick, EntityManagerFactory emf, JmsTemplate jmsTemplate) {
        this.queries_ = queries_;
        this.imageMagick = imageMagick;
        this.emf = emf;
        this.jmsTemplate = jmsTemplate;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        Files.createDirectories(thumbnailsDir);

        String directory = args.getSourceArgs().length == 1 ? args.getSourceArgs()[0] : ".";
        Path sourceDir = Path.of(directory);

        AtomicInteger counter = new AtomicInteger();
        long start = System.currentTimeMillis();

        try (Stream<Path> images = Files.walk(sourceDir)
                .parallel()
                .filter(Files::isRegularFile)
                .filter(Initializr::isImage)
        ) {
            images.forEach(image -> {
                String hash = NewMediaListener.hash(image);
                if (hash == null) {
                    System.err.println("Could not compute hash for image : " + image.toAbsolutePath().toString());
                    return;
                }
                String filename = image.getFileName().toString();

                if (!queries_.existsByFilenameAndHash(filename, hash)) {
                    jmsTemplate.convertAndSend("media", Map.of("uri", image.toUri().toString(), "hash", hash));
                }
            });

            long end = System.currentTimeMillis();
            System.out.println("Converted " + counter + " images to thumbnails. Took " + ((end - start) * 0.001) + "seconds");
        }
    }


    private static boolean isImage(Path path) {
        try {
            String mimeType = Files.probeContentType(path);
            return mimeType != null && mimeType.contains("image");
        } catch (IOException e) {
            return false;
        }
    }


}
