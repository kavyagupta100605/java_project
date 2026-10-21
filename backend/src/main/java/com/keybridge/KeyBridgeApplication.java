package com.keybridge;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class KeyBridgeApplication {

    public static void main(String[] args) {
        SpringApplication.run(KeyBridgeApplication.class, args);
    }
}
