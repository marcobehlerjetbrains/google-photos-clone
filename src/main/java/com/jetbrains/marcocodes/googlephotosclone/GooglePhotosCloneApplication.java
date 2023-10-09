package com.jetbrains.marcocodes.googlephotosclone;

import jakarta.persistence.EntityManager;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Scope;

import java.util.concurrent.atomic.AtomicInteger;

@SpringBootApplication
public class GooglePhotosCloneApplication {


    @Bean
    @Scope("singleton")
    public Archiver archiver() {
        return new Archiver();
    }


    @Bean
    public Queries queries(EntityManager em) {
        return new Queries_(em);
    }


    public static void main(String[] args) {
        SpringApplication.run(GooglePhotosCloneApplication.class, args);
    }

}
