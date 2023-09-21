package com.jetbrains.marcocodes.googlephotosclone;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Entity
public class Media {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String hash;
    private String filename;
    private LocalDateTime creationDate;

    private String originalFile;

    @Embedded
    private Location location;

    public Media() {
    }

    public Media(String hash, String filename, LocalDateTime creationDate,String originalFile, Location location) {
        this.hash = hash;
        this.filename = filename;
        this.creationDate = creationDate;
        this.originalFile = originalFile;
        this.location = location;
    }


    public static Map<LocalDate, List<Media>> toMap(List<Media> media) {
        Map<LocalDate, List<Media>> result = new LinkedHashMap<>();
        media.forEach(m -> {
            LocalDate creationDate = m.getCreationDate().toLocalDate();
            result.putIfAbsent(creationDate, new ArrayList<>());
            result.get(creationDate).add(m);
        });
        return result;
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

    public String getOriginalFile() {
        return originalFile;
    }

    public void setOriginalFile(String originalFile) {
        this.originalFile = originalFile;
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
            return location.getCountry() + ", " + location.getCity();
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
