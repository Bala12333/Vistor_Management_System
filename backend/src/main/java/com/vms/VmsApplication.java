package com.vms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableScheduling
@EnableAsync
@EnableJpaAuditing
public class VmsApplication {

    public static void main(String[] args) {
        SpringApplication.run(VmsApplication.class, args);
    }

}
