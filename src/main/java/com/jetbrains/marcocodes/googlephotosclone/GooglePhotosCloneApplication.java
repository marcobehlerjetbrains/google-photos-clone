package com.jetbrains.marcocodes.googlephotosclone;

import jakarta.persistence.EntityManager;
import org.apache.activemq.artemis.core.server.embedded.EmbeddedActiveMQ;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;

import java.util.concurrent.atomic.AtomicInteger;

@SpringBootApplication
public class GooglePhotosCloneApplication {


    @Bean
    @Scope(value = "session", proxyMode= ScopedProxyMode.TARGET_CLASS)
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
