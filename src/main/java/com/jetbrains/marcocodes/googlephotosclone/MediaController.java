package com.jetbrains.marcocodes.googlephotosclone;

import org.hibernate.query.Order;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Controller
public class MediaController {


    static String userHome = System.getProperty("user.home");
    static Path thumbnailsDir = Path.of(userHome).resolve(".photos");

    private final MediaRepository mediaRepository;
    private final Queries_ queries_;


    public MediaController(MediaRepository mediaRepository, Queries_ queries) {
        this.mediaRepository = mediaRepository;
        queries_ = queries;
    }

    @GetMapping("/")
    public String index(Model model) {
        Map<LocalDate, List<String>> images = new TreeMap<>();

        List<Media> media = queries_.findAll(Order.desc(Media_.creationDate));
        //List<Media> media = mediaRepository.findAllByOrderByCreationDateDesc();

        media.forEach(m -> {
            LocalDate creationDate = m.getCreationDate().toLocalDate();
            images.putIfAbsent(creationDate, new ArrayList<>());
            images.get(creationDate).add(m.getHash());
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

