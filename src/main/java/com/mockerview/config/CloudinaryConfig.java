package com.mockerview.config;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class CloudinaryConfig {

    @Value("${cloudinary.cloud-name}")
    private String cloudName;

    @Value("${cloudinary.api-key}")
    private String apiKey;

    @Value("${cloudinary.api-secret}")
    private String apiSecret;

    @Bean
    public Cloudinary cloudinary() {
        log.info("🔧 ========================================");
        log.info("🔧 Cloudinary 초기화 시작");
        log.info("🔧 Cloud Name: {}", cloudName);
        log.info("🔧 API Key: {}", apiKey);
        log.info("🔧 API Secret: {}...", apiSecret != null ? apiSecret.substring(0, Math.min(10, apiSecret.length())) : "null");
        log.info("🔧 ========================================");
        
        Cloudinary cloudinary = new Cloudinary(ObjectUtils.asMap(
            "cloud_name", cloudName,
            "api_key", apiKey,
            "api_secret", apiSecret,
            "secure", true
        ));
        
        log.info("✅ Cloudinary Bean 생성 완료");
        return cloudinary;
    }
}