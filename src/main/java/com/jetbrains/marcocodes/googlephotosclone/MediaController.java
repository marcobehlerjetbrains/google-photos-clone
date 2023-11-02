package com.jetbrains.marcocodes.googlephotosclone;

import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Controller
public class MediaController {

    static String userHome = System.getProperty("user.home");
    static Path thumbnailsDir = Path.of(userHome).resolve(".photos");

    private final Queries queries_;
    private final Archiver archiver;

    public MediaController(Queries queries_, Archiver archiver) {
        this.queries_ = queries_;
        this.archiver = archiver;
    }

    @GetMapping("/")
    public String index(Model model) {
        List<Media> media = queries_.mediaSeek();
        model.addAttribute("archiver", archiver);
        model.addAttribute("datesToMedia", Media.toMap(media));
        return "index";
    }


    @GetMapping("/seek")
    public String index(Model model, @RequestParam @DateTimeFormat(pattern = "yyyyMMddHHmmss") LocalDateTime date, @RequestParam Long id) {
        List<Media> media = queries_.mediaSeek(date, id);
        model.addAttribute("datesToMedia", Media.toMap(media));
        return "seek";
    }

    @GetMapping("/a/{hash}")
    @ResponseBody
    public ResponseEntity<Resource> download(@PathVariable String hash, @RequestParam(value="format",required=false) String format) {
        String extension = format != null && format.equalsIgnoreCase("png") ? "png" : "webp";
        Path media = thumbnailsDir.resolve(hash.substring(0, 2)).resolve(hash.substring(2) + "." + extension);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.valueOf("image/" + extension));
        return ResponseEntity.ok().headers(headers).body(new PathResource(media));
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
        List<Path> sourceFiles = queries_.mediaSeek()
                .stream()
                .map(Media::getOriginalFile)
                .map(s -> {
                    try {
                        return Path.of(new URI(s));
                    } catch (URISyntaxException e) {
                        throw new RuntimeException(e);
                    }
                }).collect(Collectors.toCollection(ArrayList::new));

        archiver.run(sourceFiles);
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


    @GetMapping("/media/archive/download")
    @ResponseBody
    public ResponseEntity<Resource> downloadArchive() {
        if (archiver.getArchive() == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        Resource file = new PathResource(archiver.getArchive());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getFilename() + "\"")
                .body(file);
    }
}

