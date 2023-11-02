package com.jetbrains.marcocodes.googlephotosclone;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Stream;

@RestController
public class MediaRestController {


    private final Queries queries;

    public MediaRestController(Queries queries) {
        this.queries = queries;
    }

    @GetMapping("/media")
    public Stream<MediaDto> media(@RequestParam(required = false) String format) {
        String extension = format != null && format.equals("png") ? "png" : "webp";
        return queries.mediaSeek().stream().map(media -> new MediaDto("http://192.168.178.39:8080/a/" + media.getHash() + "?format=" + extension));
    }
}
