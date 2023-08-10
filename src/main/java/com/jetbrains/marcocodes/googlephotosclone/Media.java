package com.jetbrains.marcocodes.googlephotosclone;

import org.springframework.data.annotation.Id;

import java.time.LocalDateTime;

public record Media(@Id  Long id, String hash, String filename, LocalDateTime creationDate) {


}
