package com.nocountry.authservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class AuthServiceApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApiApplication.class, args);
    }
}
