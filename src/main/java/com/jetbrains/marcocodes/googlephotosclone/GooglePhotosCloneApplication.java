package com.jetbrains.marcocodes.googlephotosclone;

import jakarta.persistence.EntityManager;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import javax.swing.text.html.parser.Entity;

@SpringBootApplication
public class GooglePhotosCloneApplication {

    @Bean
    public Queries_ queries_(EntityManager em) {
        return new Queries_(em);
    }

    public static void main(String[] args) {
        SpringApplication.run(GooglePhotosCloneApplication.class, args);
    }

}
