package com.mockerview;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class MockerviewApplication {
    
    static {
        try {
            Dotenv dotenv = Dotenv.configure()
                .directory(".")
                .filename(".env")
                .load();
            
            dotenv.entries().forEach(entry -> {
                System.setProperty(entry.getKey(), entry.getValue());
            });
            
            System.out.println("환경변수 로드 성공");
        } catch (Exception e) {
            System.err.println("환경변수 로드 실패: " + e.getMessage());
        }
    }
    
    public static void main(String[] args) {
        SpringApplication.run(MockerviewApplication.class, args);
    }
}