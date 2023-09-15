
package com.jetbrains.marcocodes.googlephotosclone;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Entity
public class MediaTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String filename;

    @Enumerated(value = EnumType.STRING)
    private Status status = Status.PENDING;

    public MediaTask() {
    }

    public MediaTask(String filename) {
        this.filename = filename;
    }

    public enum Status {
        PENDING, PROCESSED
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }
}
