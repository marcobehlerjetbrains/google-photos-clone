package com.jetbrains.marcocodes.googlephotosclone;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDateTime;

@JsonIgnoreProperties(ignoreUnknown=true)
public record TestMetadata(Integer width, Integer height, LocalDateTime date, String latitude, String longitude) {
}
