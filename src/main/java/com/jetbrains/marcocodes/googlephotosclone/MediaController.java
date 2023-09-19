package com.jetbrains.marcocodes.googlephotosclone;

import jakarta.persistence.EntityManager;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
    public String index(Model model) {
        List<Media> media = queries_.mediaSeek();
        model.addAttribute("archiver", new Archiver());
        model.addAttribute("images", Media.toMap(media));
        return "index";
    }


    @GetMapping("/seek")
    public String index(Model model, @RequestParam @DateTimeFormat(pattern = "yyyyMMddHHmmss") LocalDateTime date, @RequestParam Long id) {
        List<Media> media = queries_.mediaSeek(date, id);
        model.addAttribute("images", Media.toMap(media));
        return "seek";
    }

    @GetMapping("/a/{hash}")
    @ResponseBody
    public Resource download(@PathVariable String hash) {
        Path media = thumbnailsDir.resolve(hash.substring(0, 2)).resolve(hash.substring(2) + ".webp");
        return new PathResource(media);
    }


    @GetMapping("/b/{hash}")
    @ResponseBody
    public HttpEntity<Resource> downloadB(@PathVariable String hash) {
        Media media1 = queries_.mediaByHash(hash);
        if (media1 == null) throw new ResponseStatusException(HttpStatusCode.valueOf(404));
        try {

            HttpHeaders header = new HttpHeaders();
            header.setContentType(MediaType.IMAGE_JPEG);
            Path path = Path.of(new URI(media1.getOriginalFile()));
            return new HttpEntity<>(new PathResource(path),
                    header);
        } catch (URISyntaxException e) {
            e.printStackTrace();
            throw new ResponseStatusException(HttpStatusCode.valueOf(404));
        }

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

