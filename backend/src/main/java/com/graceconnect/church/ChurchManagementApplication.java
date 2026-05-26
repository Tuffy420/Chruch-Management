package com.graceconnect.church;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ChurchManagementApplication {
    public static void main(String[] args) {
        SpringApplication.run(ChurchManagementApplication.class, args);
    }
}
