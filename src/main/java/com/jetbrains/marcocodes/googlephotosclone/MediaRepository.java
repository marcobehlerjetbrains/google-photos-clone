package com.jetbrains.marcocodes.googlephotosclone;

import org.springframework.data.repository.ListCrudRepository;

import java.util.List;

public interface MediaRepository extends ListCrudRepository<Media, Long> {
    boolean existsByFilenameAndHash(String filename, String hash);

    List<Media> findAllByOrderByCreationDateDesc();
}
