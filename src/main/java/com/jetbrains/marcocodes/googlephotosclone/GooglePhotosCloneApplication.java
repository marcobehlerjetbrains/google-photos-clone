package com.jetbrains.marcocodes.googlephotosclone;

import jakarta.persistence.EntityManager;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

@SpringBootApplication
public class GooglePhotosCloneApplication {




    @Bean
    public Archiver archiver() {
        return new Archiver();
    }


    @Bean
    public ExecutorService executorService() {
        return Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    }


    @Bean
    public Queries queries(EntityManager em) {
        return new Queries_(em);
    }


    public static void main(String[] args) {
        SpringApplication.run(GooglePhotosCloneApplication.class, args);
    }

}
