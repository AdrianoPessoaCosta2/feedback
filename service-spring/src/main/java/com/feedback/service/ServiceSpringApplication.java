package com.feedback.service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ServiceSpringApplication {

    public static void main(String[] args) {
        SpringApplication.run(ServiceSpringApplication.class, args);
    }
}
