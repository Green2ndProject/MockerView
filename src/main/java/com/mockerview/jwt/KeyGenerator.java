package com.mockerview.jwt;

import java.security.SecureRandom;
import java.util.Base64;

public class KeyGenerator {
  
    public static void main(String[] args) {
        SecureRandom random = new SecureRandom();
        byte[] keyBytes = new byte[32]; // 256 bits = 32 bytes
        random.nextBytes(keyBytes);
        String secretKey = Base64.getEncoder().encodeToString(keyBytes);
        System.out.println(secretKey);
    
    }
}
