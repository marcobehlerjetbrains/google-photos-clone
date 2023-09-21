package com.jetbrains.marcocodes.googlephotosclone;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Transient;

@Embeddable
public class Location {
    private Double latitude = 0.0;
    private Double longitude = 0.0;
    private String country;
    private String city;

    @Transient
    private String dms;

    public Location() {
    }

    public Location(Double latitude, Double longitude, String country, String city, String dms) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.country = country;
        this.city = city;
        this.dms = dms;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getDms() {
        return dms;
    }

    public void setDms(String dms) {
        this.dms = dms;
    }
}
