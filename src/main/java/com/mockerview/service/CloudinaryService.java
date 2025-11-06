package com.mockerview.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CloudinaryService {

    private final Cloudinary cloudinary;

    public String uploadVideo(MultipartFile file, String sessionId) throws IOException {
        long startTime = System.currentTimeMillis();
        try {
            double fileSizeMB = file.getSize() / 1024.0 / 1024.0;
            log.info("📹 비디오 업로드 시작 - sessionId: {}, 파일명: {}, 크기: {}MB", 
                sessionId, file.getOriginalFilename(), fileSizeMB);

            if (file.isEmpty()) {
                throw new IOException("파일이 비어있습니다");
            }

            log.info("⏳ Cloudinary Signed Upload 시작...");
            
            Map uploadResult = cloudinary.uploader().upload(file.getBytes(),
                ObjectUtils.asMap(
                    "resource_type", "video",
                    "folder", "mockerview/recordings/" + sessionId,
                    "public_id", "recording_" + System.currentTimeMillis(),
                    "chunk_size", 6000000,
                    "timeout", 300000
                ));

            String videoUrl = (String) uploadResult.get("secure_url");
            long uploadTime = System.currentTimeMillis() - startTime;
            log.info("✅ 비디오 업로드 완료 - URL: {}, 소요시간: {}ms", videoUrl, uploadTime);

            return videoUrl;
            
        } catch (Exception e) {
            long failTime = System.currentTimeMillis() - startTime;
            log.error("❌ 비디오 업로드 실패 - sessionId: {}, 소요시간: {}ms, 에러타입: {}, 메시지: {}", 
                sessionId, failTime, e.getClass().getSimpleName(), e.getMessage());
            log.error("❌ 상세 스택트레이스:", e);
            throw new IOException("비디오 업로드 실패: " + e.getMessage(), e);
        }
    }

    public String uploadVideoFromUrl(String videoUrl, String sessionId) throws IOException {
        try {
            log.info("📹 URL에서 비디오 업로드 시작 - sessionId: {}, URL: {}", sessionId, videoUrl);

            Map uploadResult = cloudinary.uploader().upload(videoUrl,
                ObjectUtils.asMap(
                    "resource_type", "video",
                    "folder", "mockerview/recordings/" + sessionId,
                    "public_id", "recording_" + System.currentTimeMillis(),
                    "chunk_size", 6000000
                ));

            String secureUrl = (String) uploadResult.get("secure_url");
            log.info("✅ URL 비디오 업로드 완료 - URL: {}", secureUrl);

            return secureUrl;
        } catch (Exception e) {
            log.error("❌ URL 비디오 업로드 실패 - sessionId: {}", sessionId, e);
            throw new IOException("URL 비디오 업로드 실패: " + e.getMessage(), e);
        }
    }

    public void deleteVideo(String publicId) {
        try {
            log.info("🗑️ 비디오 삭제 시작 - publicId: {}", publicId);
            cloudinary.uploader().destroy(publicId, ObjectUtils.asMap("resource_type", "video"));
            log.info("✅ 비디오 삭제 완료");
        } catch (Exception e) {
            log.error("❌ 비디오 삭제 실패 - publicId: {}", publicId, e);
        }
    }
}