package com.jetbrains.marcocodes.googlephotosclone;

import jakarta.persistence.EntityManager;
import org.hibernate.annotations.processing.HQL;
import org.hibernate.annotations.processing.SQL;

import java.time.LocalDateTime;
import java.util.List;

public interface Queries {

    EntityManager entityManager();

    @HQL("select count(m) > 0 from Media m where m.filename = :filename and m.hash = :hash")
    Boolean existsByFilenameAndHash(String filename, String hash);

    @HQL("from Media m where m.hash = :hash order by m.creationDate desc fetch first 1 rows only ")
    Media mediaByHash(String hash);

    @HQL("from Media m order by m.creationDate desc, id desc fetch first 20 rows only ")
    List<Media> mediaSeek();

    @SQL("select * from MEDIA m where (m.creation_date, m.id) < (:creationDate, :id) order by m.creation_date desc, id desc fetch first 20 rows only")
    List<Media> mediaSeek(LocalDateTime creationDate, Long id);;
}
