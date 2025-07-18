package com.example.ad_manager;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@SpringBootApplication
public class AdManagerApplication {
    public static void main(String[] args) { SpringApplication.run(AdManagerApplication.class, args); }
}
