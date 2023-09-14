package com.jetbrains.marcocodes.googlephotosclone;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.Order;
import org.hibernate.query.criteria.CriteriaDefinition;
import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.awt.print.Book;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Controller
public class MediaController {

    static String userHome = System.getProperty("user.home");
    static Path thumbnailsDir = Path.of(userHome).resolve(".photos");

    private final Queries queries_;

    private final EntityManager entityManager;

    public MediaController(Queries queries_, EntityManager entityManager) {
        this.queries_ = queries_;
        this.entityManager = entityManager;
    }

    @GetMapping("/")
    public String index(Model model, @RequestParam(required = false) @DateTimeFormat(pattern = "yyyyMMddHHmmss") LocalDateTime date, @RequestParam(required = false) Long id) {
        Map<LocalDate, List<Media>> images = new LinkedHashMap<>();

        List<Media> media = List.of();
        if (date == null || id == null) {
            var q =
                    "from Media m " +
                            "order by m.creationDate desc, id desc " +
                            "fetch first 20 rows only";
            media = entityManager.createQuery(q, Media.class).getResultList();
        } else {
            var q =
                    "select * from MEDIA m " +
                            "where (m.creation_date, m.id) < (:date, :id) " +
                            "order by m.creation_date desc, id desc " +
                            "fetch first 20 rows only";
            media = entityManager.createNativeQuery(q, Media.class)
                    .setParameter("date", date)
                    .setParameter("id", id)
                    .getResultList();
        }
/*
        CriteriaQuery<Media> query = new CriteriaDefinition<>(entityManager, Media.class, q) {{
            Root<Media> root = (Root<Media>) getRootList().get(0); // a bit ugly
            if (date != null && id != null) {
                restrict(lessThan(root.get(Media_.creationDate), date));
                restrict(lessThan(root.get(Media_.id), id));
            }
        }};*/


        media.forEach(m -> {
            LocalDate creationDate = m.getCreationDate().toLocalDate();
            images.putIfAbsent(creationDate, new ArrayList<>());
            images.get(creationDate).add(m);
        });

        model.addAttribute("images", images);
        return "index";
    }

    @GetMapping("/a/{hash}")
    @ResponseBody
    public Resource download(@PathVariable String hash) {
        Path media = thumbnailsDir.resolve(hash.substring(0, 2)).resolve(hash.substring(2) + ".webp");
        return new PathResource(media);
    }
}

