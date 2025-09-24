package com.mockerview;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class MockerviewApplication {
    public static void main(String[] args) {
        SpringApplication.run(MockerviewApplication.class, args);
    }
}