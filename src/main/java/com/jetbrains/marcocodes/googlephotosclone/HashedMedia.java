package com.jetbrains.marcocodes.googlephotosclone;

import java.nio.file.Path;

public record HashedMedia(Path path, String hash) {

    public HashedMedia(String uri, String hash) {
        this(Path.of(uri), hash);
    }
}
