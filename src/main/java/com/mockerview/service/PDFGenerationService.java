package com.mockerview.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.mockerview.entity.InterviewReport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.*;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PDFGenerationService {

    private final Cloudinary cloudinary;

    public String generatePDF(InterviewReport report) {
        log.info("📄 PDF 생성 시작: Report ID {}", report.getId());

        try {
            String htmlContent = generateHTMLContent(report);
            byte[] pdfBytes = convertHTMLToPDF(htmlContent);
            String pdfUrl = uploadToCloudinary(pdfBytes, report.getId());

            log.info("✅ PDF 생성 완료: {}", pdfUrl);
            return pdfUrl;
        } catch (Exception e) {
            log.error("❌ PDF 생성 실패: {}", e.getMessage(), e);
            return null;
        }
    }

    private String generateHTMLContent(InterviewReport report) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy년 MM월 dd일");
        String date = report.getCreatedAt().format(formatter);

        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8"/>
                <style>
                    * {
                        margin: 0;
                        padding: 0;
                        box-sizing: border-box;
                    }
                    
                    body {
                        font-family: 'Malgun Gothic', '맑은 고딕', sans-serif;
                        color: #333;
                        padding: 40px;
                        background: #ffffff;
                    }
                    
                    .header {
                        text-align: center;
                        margin-bottom: 40px;
                        padding-bottom: 20px;
                        border-bottom: 3px solid #4A90E2;
                    }
                    
                    .logo {
                        font-size: 28px;
                        font-weight: 700;
                        color: #4A90E2;
                        margin-bottom: 10px;
                    }
                    
                    h1 {
                        font-size: 24px;
                        color: #333;
                        margin-bottom: 10px;
                    }
                    
                    .date {
                        color: #666;
                        font-size: 14px;
                    }
                    
                    .score-section {
                        background: #F8F9FA;
                        padding: 20px;
                        margin: 20px 0;
                        border-left: 4px solid #4A90E2;
                    }
                    
                    .score-title {
                        font-size: 18px;
                        font-weight: 700;
                        color: #4A90E2;
                        margin-bottom: 15px;
                    }
                    
                    .score-grid {
                        display: grid;
                        grid-template-columns: repeat(2, 1fr);
                        gap: 15px;
                    }
                    
                    .score-item {
                        background: white;
                        padding: 15px;
                        border: 1px solid #E0E0E0;
                    }
                    
                    .score-label {
                        font-size: 12px;
                        color: #666;
                        margin-bottom: 5px;
                    }
                    
                    .score-value {
                        font-size: 24px;
                        font-weight: 700;
                        color: #4A90E2;
                    }
                    
                    .section {
                        margin: 30px 0;
                    }
                    
                    .section-title {
                        font-size: 18px;
                        font-weight: 700;
                        color: #333;
                        margin-bottom: 15px;
                        padding-bottom: 10px;
                        border-bottom: 2px solid #E0E0E0;
                    }
                    
                    .content-box {
                        background: #FAFAFA;
                        padding: 20px;
                        line-height: 1.8;
                        font-size: 14px;
                    }
                    
                    .footer {
                        margin-top: 50px;
                        text-align: center;
                        color: #999;
                        font-size: 12px;
                        padding-top: 20px;
                        border-top: 1px solid #E0E0E0;
                    }
                </style>
            </head>
            <body>
                <div class="header">
                    <div class="logo">MockerView</div>
                    <h1>면접 종합 리포트</h1>
                    <div class="date">%s</div>
                </div>
                
                <div class="score-section">
                    <div class="score-title">종합 점수</div>
                    <div class="score-grid">
                        <div class="score-item">
                            <div class="score-label">전체 점수</div>
                            <div class="score-value">%d점</div>
                        </div>
                        <div class="score-item">
                            <div class="score-label">커뮤니케이션</div>
                            <div class="score-value">%d점</div>
                        </div>
                        <div class="score-item">
                            <div class="score-label">기술력</div>
                            <div class="score-value">%d점</div>
                        </div>
                        <div class="score-item">
                            <div class="score-label">자신감</div>
                            <div class="score-value">%d점</div>
                        </div>
                    </div>
                </div>
                
                <div class="section">
                    <div class="section-title">전체 평가</div>
                    <div class="content-box">%s</div>
                </div>
                
                <div class="section">
                    <div class="section-title">주요 강점</div>
                    <div class="content-box">%s</div>
                </div>
                
                <div class="section">
                    <div class="section-title">개선할 점</div>
                    <div class="content-box">%s</div>
                </div>
                
                <div class="section">
                    <div class="section-title">추천 사항</div>
                    <div class="content-box">%s</div>
                </div>
                
                <div class="section">
                    <div class="section-title">상세 분석</div>
                    <div class="content-box">%s</div>
                </div>
                
                <div class="footer">
                    MockerView - AI 모의면접 플랫폼<br/>
                    본 리포트는 AI 분석을 기반으로 생성되었습니다.
                </div>
            </body>
            </html>
            """,
                date,
                report.getOverallScore(),
                report.getCommunicationScore(),
                report.getTechnicalScore(),
                report.getConfidenceScore(),
                report.getOverallInsights(),
                report.getStrengths(),
                report.getWeaknesses(),
                report.getRecommendations(),
                report.getDetailedAnalysis()
        );
    }

    private byte[] convertHTMLToPDF(String htmlContent) throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        ITextRenderer renderer = new ITextRenderer();
        renderer.setDocumentFromString(htmlContent);
        renderer.layout();
        renderer.createPDF(outputStream);

        return outputStream.toByteArray();
    }

    private String uploadToCloudinary(byte[] pdfBytes, Long reportId) throws Exception {
        Map uploadResult = cloudinary.uploader().upload(pdfBytes, ObjectUtils.asMap(
                "resource_type", "raw",
                "folder", "mockerview/reports",
                "public_id", "report_" + reportId,
                "format", "pdf"
        ));

        return (String) uploadResult.get("secure_url");
    }
}
