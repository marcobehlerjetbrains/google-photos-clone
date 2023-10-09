package com.jetbrains.marcocodes.googlephotosclone;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class Archiver {

    private double progress = 0.0;
    private String status = "Waiting";

    private Path archive;

    public String status() {
        return status;
    }

    public double progress() {
        if (progress >= 1.0) {
            status = "Complete";
        }
        return progress;
    }

    public void run(List<Path> srcFiles) {
        this.status = "Running";

        new Thread(() -> {
            try {
                archive = Files.createTempFile("archive", ".zip");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(archive))) {
                long totalFileSize = srcFiles.stream().map(path -> {
                    try {
                        return Files.size(path);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }).reduce(0L, Long::sum);

                AtomicLong compressed = new AtomicLong();

                srcFiles.forEach(path -> {
                    if (!Files.exists(path)) {
                        throw new RuntimeException("problem");
                    }

                    try (InputStream is = Files.newInputStream(path)) {
                        ZipEntry entry = new ZipEntry(path.getFileName().toString());
                        zos.putNextEntry(entry);

                        byte[] buffer = new byte[8192];
                        int read;
                        while ((read = is.read(buffer, 0, 8192)) >= 0) {
                            ((OutputStream) zos).write(buffer, 0, read);
                            compressed.addAndGet(read);
                            progress = (double) compressed.get() / totalFileSize;
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).start();
    }

    public void reset() {
        this.status = "Waiting";
        this.progress = 0.0;
        this.archive = null;
    }

    public Path getArchive() {
        return archive;
    }
}
