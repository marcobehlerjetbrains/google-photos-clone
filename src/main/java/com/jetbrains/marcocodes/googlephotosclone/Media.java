package com.jetbrains.marcocodes.googlephotosclone;

import jakarta.persistence.*;


import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@NamedQuery(name = "#findMeSomeMore", query = "select m from Media m order by m.creationDate desc")
public class Media {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String hash;
    private String filename;
    private LocalDateTime creationDate;

    public Media() {
    }

    public Media(Long id, String hash, String filename, LocalDateTime creationDate) {
        this.id = id;
        this.hash = hash;
        this.filename = filename;
        this.creationDate = creationDate;
    }


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public LocalDateTime getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(LocalDateTime creationDate) {
        this.creationDate = creationDate;
    }
}
