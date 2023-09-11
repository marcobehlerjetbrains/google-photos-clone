package com.jetbrains.marcocodes.googlephotosclone;

import jakarta.persistence.EntityManager;
import org.hibernate.annotations.processing.Find;
import org.hibernate.annotations.processing.HQL;
import org.hibernate.query.Order;

import java.util.List;

public interface Queries {

    EntityManager entityManager();

    @HQL("select count(m) > 0 from Media m where m.filename = :filename and m.hash = :hash")
    Boolean existsByFilenameAndHash(String filename, String hash);

    @HQL("from Media")
    List<Media> media(Order<Media>... orders);

    @Find
    Media mediaByName(String filename, String hash);
}
