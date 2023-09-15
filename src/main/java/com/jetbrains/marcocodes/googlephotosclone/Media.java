package com.jetbrains.marcocodes.googlephotosclone;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

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

    private String location;

    private Double longitude = 0.0;

    private Double latitude = 0.0;

    public Media() {
    }

    public Media(String hash, String filename, LocalDateTime creationDate, Location location) {
        this.hash = hash;
        this.filename = filename;
        this.creationDate = creationDate;
        if (location != null) {
            this.longitude = location.longitude();
            this.latitude = location.latitude();
            this.location = location.name();
        }
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

    public void setCreationDate(LocalDateTime creationDate) {
        this.creationDate = creationDate;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public static String locationsToString(Collection<Media> media) {
        Set<String> locations = media.stream().map(m -> {
            String location = m.getLocation();
            if (location == null) {
                return "Unknown";
            }
            return location.substring(location.indexOf(",") + 1) + ", " + location.substring(0, location.indexOf(","));
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
