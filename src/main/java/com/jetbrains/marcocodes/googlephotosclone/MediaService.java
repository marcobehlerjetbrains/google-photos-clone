package com.jetbrains.marcocodes.googlephotosclone;

import com.drew.imaging.png.PngChunkType;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.exif.GpsDirectory;
import com.drew.metadata.jpeg.JpegDirectory;
import com.drew.metadata.png.PngDirectory;
import io.github.rctcwyvrn.blake3.Blake3;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.LockModeType;
import org.hibernate.LockOptions;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;

@Service
public class MediaService {

    static String userHome = System.getProperty("user.home");
    static Path thumbnailsDir = Path.of(userHome).resolve(".photos");

    private final ExecutorService executorService;

    private final EntityManagerFactory emf;

    private final EntityManager em;

    private final ImageMagick imageMagick;

    public MediaService(ExecutorService executorService, EntityManagerFactory emf, EntityManager em, ImageMagick imageMagick) {
        this.executorService = executorService;

        this.emf = emf;
        this.em = em;
        this.imageMagick = imageMagick;
    }

    public List<MediaTask> getTasks(
            EntityManager entityManager) {
        return entityManager.createQuery(
                        "select p " +
                        "from MediaTask p " +
                        "where p.status = :status " +
                        "order by p.id " +
                        "fetch first 50 rows only", MediaTask.class)
                .setParameter("status", MediaTask.Status.PENDING)
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .setHint(
                        "javax.persistence.lock.timeout",
                        LockOptions.NO_WAIT
                )
                .getResultList();
    }

    public void process() {
        List<MediaTask> tasks = getTasks(em);

        while (!tasks.isEmpty()) {

        }

          /*  images.forEach(image -> executorService.submit(() -> {
                emf.unwrap(SessionFactory.class).inTransaction(em -> {
                    try {
                        String hash = hash(image);
                        String filename = image.getFileName().toString();

                        if (!queries_.existsByFilenameAndHash(filename, hash)) {

                            try (InputStream is = Files.newInputStream(image)) {
                                Metadata metadata = ImageMetadataReader.readMetadata(is);
                                Dimensions dimensions = getImageSize(image, metadata);
                                Location location = getLocation(image, metadata);
                                LocalDateTime creationTime = creationTime(image, metadata);

                                final boolean success = createThumbnail(image, hash, dimensions);
                                if (success) {
                                    counter.incrementAndGet();
                                    Media media = new Media(hash, filename, creationTime, location);
                                    em.persist(media);
                                }
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });


            }));
        }*/
    }







}
