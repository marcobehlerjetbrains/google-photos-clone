package com.jetbrains.marcocodes.googlephotosclone;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Entity
public class Media {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String hash;
    private String filename;
    private LocalDateTime creationDate;

    @Embedded
    private Location location;

    public Media() {
    }

    public Media(String hash, String filename, LocalDateTime creationDate, Location location) {
        this.hash = hash;
        this.filename = filename;
        this.creationDate = creationDate;
        this.location = location;
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

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public static String locationsToString(Collection<Media> media) {
        Set<String> locations = media.stream().map(m -> {
            Location location = m.getLocation();
            if (location == null) {
                return "Unknown";
            }
            return location.getCountry() + " ," + location.getCity();
        }).collect(Collectors.toSet());
        if (locations.size() > 1) {
            return locations.iterator().next() + "& " + (locations.size() - 1) + " more";
        } else if (locations.size() == 1) {
            return locations.iterator().next();
        } else {
            throw new UnsupportedOperationException("should not happen");
        }
    }
}
