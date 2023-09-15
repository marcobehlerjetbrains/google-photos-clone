package com.jetbrains.marcocodes.googlephotosclone;

import jakarta.persistence.EntityManager;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
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

    private final Archiver archiver;

    public MediaController(Queries queries_, EntityManager entityManager, Archiver archiver) {
        this.queries_ = queries_;
        this.entityManager = entityManager;
        this.archiver = archiver;
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

        media.forEach(m -> {
            LocalDate creationDate = m.getCreationDate().toLocalDate();
            images.putIfAbsent(creationDate, new ArrayList<>());
            images.get(creationDate).add(m);
        });

        model.addAttribute("archiver", new Archiver());
        model.addAttribute("images", images);
        return "index";
    }

    @GetMapping("/a/{hash}")
    @ResponseBody
    public Resource download(@PathVariable String hash) {
        Path media = thumbnailsDir.resolve(hash.substring(0, 2)).resolve(hash.substring(2) + ".webp");
        return new PathResource(media);
    }


    @PostMapping("/media/archive")
    public String archive(Model model) {
        archiver.run();
        model.addAttribute("archiver", archiver);
        return "archive_ui";
    }

    @GetMapping("/media/archive")
    public String archiveGet(Model model) {
        model.addAttribute("archiver", archiver);
        return "archive_ui";
    }

    @DeleteMapping("/media/archive")
    public String delete(Model model) {
        archiver.reset();
        model.addAttribute("archiver", archiver);
        return "archive_ui";
    }


    @GetMapping("/media/archive/file")
    public ResponseEntity<Resource> dd(String param) {
        Resource file = new PathResource(Path.of("c:\\tmp\\aaa\\Uganda.zip"));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getFilename() + "\"")
                .body(file);
    }

}

