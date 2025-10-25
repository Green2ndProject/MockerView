package com.mockerview.controller.api;

import com.cloudinary.Cloudinary;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/debug/cloudinary")
@RequiredArgsConstructor
public class CloudinaryDebugController {

    private final Cloudinary cloudinary;

    @Value("${cloudinary.cloud-name}")
    private String cloudName;

    @Value("${cloudinary.api-key}")
    private String apiKey;

    @Value("${cloudinary.api-secret}")
    private String apiSecret;

    @GetMapping("/config")
    public ResponseEntity<?> checkConfig() {
        log.info("🔍 Cloudinary 설정 확인");
        
        Map<String, Object> config = cloudinary.config.asMap();
        
        return ResponseEntity.ok(Map.of(
            "cloudName", cloudName != null ? cloudName : "null",
            "apiKey", apiKey != null ? apiKey : "null",
            "apiSecret", apiSecret != null ? (apiSecret.substring(0, 10) + "...") : "null",
            "beanConfig", Map.of(
                "cloud_name", config.get("cloud_name"),
                "api_key", config.get("api_key"),
                "api_secret_set", config.get("api_secret") != null
            ),
            "status", "OK"
        ));
    }
}
