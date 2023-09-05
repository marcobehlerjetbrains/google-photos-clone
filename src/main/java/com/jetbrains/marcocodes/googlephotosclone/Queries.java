package com.jetbrains.marcocodes.googlephotosclone;

import jakarta.persistence.EntityManager;
import org.hibernate.annotations.processing.CheckHQL;
import org.hibernate.annotations.processing.HQL;
import org.hibernate.query.Order;

import java.util.List;

public interface Queries {

    EntityManager entityManager();

    @HQL("select count(m) > 0 from Media m where m.filename = :filename and m.hash = :hash")
    public Boolean existsByFilenameAndHash(String filename, String hash);

    @HQL("select m from Media m")
    public List<Media> findAll(Order<Media>... order);

}
