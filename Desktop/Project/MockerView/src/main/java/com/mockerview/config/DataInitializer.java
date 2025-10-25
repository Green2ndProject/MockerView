package com.mockerview.config;

import com.mockerview.entity.Category;
import com.mockerview.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final CategoryRepository categoryRepository;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        if (categoryRepository.count() == 0) {
            log.info("카테고리 데이터가 없습니다. 초기 데이터를 삽입합니다...");
            initializeCategories();
            log.info("✅ 카테고리 초기 데이터 삽입 완료!");
        } else {
            log.info("카테고리 데이터가 이미 존재합니다. 건너뜁니다.");
        }
    }

    private void initializeCategories() {
        Category dev = categoryRepository.save(Category.builder()
            .code("DEV")
            .name("개발")
            .description("IT 및 소프트웨어 개발 분야")
            .categoryType("MAIN")
            .displayOrder(1)
            .isActive(true)
            .icon("💻")
            .build());

        Category design = categoryRepository.save(Category.builder()
            .code("DESIGN")
            .name("디자인")
            .description("UI/UX 및 그래픽 디자인")
            .categoryType("MAIN")
            .displayOrder(2)
            .isActive(true)
            .icon("🎨")
            .build());

        Category business = categoryRepository.save(Category.builder()
            .code("BUSINESS")
            .name("기획/경영")
            .description("사업 기획 및 경영 전략")
            .categoryType("MAIN")
            .displayOrder(3)
            .isActive(true)
            .icon("📊")
            .build());

        Category marketing = categoryRepository.save(Category.builder()
            .code("MARKETING")
            .name("마케팅")
            .description("마케팅 및 브랜딩")
            .categoryType("MAIN")
            .displayOrder(4)
            .isActive(true)
            .icon("📢")
            .build());

        Category data = categoryRepository.save(Category.builder()
            .code("DATA")
            .name("데이터")
            .description("데이터 분석 및 AI")
            .categoryType("MAIN")
            .displayOrder(5)
            .isActive(true)
            .icon("📈")
            .build());

        Category general = categoryRepository.save(Category.builder()
            .code("GENERAL")
            .name("일반")
            .description("공통 역량 및 인성")
            .categoryType("MAIN")
            .displayOrder(6)
            .isActive(true)
            .icon("👥")
            .build());

        categoryRepository.save(Category.builder()
            .code("DEV_FRONTEND")
            .name("프론트엔드")
            .description("React, Vue, Angular 등")
            .categoryType("SUB")
            .parent(dev)
            .displayOrder(1)
            .isActive(true)
            .build());

        categoryRepository.save(Category.builder()
            .code("DEV_BACKEND")
            .name("백엔드")
            .description("Java, Spring, Node.js 등")
            .categoryType("SUB")
            .parent(dev)
            .displayOrder(2)
            .isActive(true)
            .build());

        categoryRepository.save(Category.builder()
            .code("DEV_MOBILE")
            .name("모바일")
            .description("Android, iOS 개발")
            .categoryType("SUB")
            .parent(dev)
            .displayOrder(3)
            .isActive(true)
            .build());

        categoryRepository.save(Category.builder()
            .code("DEV_DEVOPS")
            .name("DevOps")
            .description("CI/CD, 클라우드, 인프라")
            .categoryType("SUB")
            .parent(dev)
            .displayOrder(4)
            .isActive(true)
            .build());

        categoryRepository.save(Category.builder()
            .code("DESIGN_UI")
            .name("UI 디자인")
            .description("사용자 인터페이스 디자인")
            .categoryType("SUB")
            .parent(design)
            .displayOrder(1)
            .isActive(true)
            .build());

        categoryRepository.save(Category.builder()
            .code("DESIGN_UX")
            .name("UX 디자인")
            .description("사용자 경험 디자인")
            .categoryType("SUB")
            .parent(design)
            .displayOrder(2)
            .isActive(true)
            .build());

        categoryRepository.save(Category.builder()
            .code("DESIGN_GRAPHIC")
            .name("그래픽")
            .description("시각 디자인 및 브랜딩")
            .categoryType("SUB")
            .parent(design)
            .displayOrder(3)
            .isActive(true)
            .build());

        categoryRepository.save(Category.builder()
            .code("BUSINESS_PLAN")
            .name("사업기획")
            .description("사업 전략 및 기획")
            .categoryType("SUB")
            .parent(business)
            .displayOrder(1)
            .isActive(true)
            .build());

        categoryRepository.save(Category.builder()
            .code("BUSINESS_PM")
            .name("프로덕트 매니저")
            .description("제품 관리 및 전략")
            .categoryType("SUB")
            .parent(business)
            .displayOrder(2)
            .isActive(true)
            .build());

        categoryRepository.save(Category.builder()
            .code("MARKETING_DIGITAL")
            .name("디지털 마케팅")
            .description("SNS, 광고, SEO")
            .categoryType("SUB")
            .parent(marketing)
            .displayOrder(1)
            .isActive(true)
            .build());

        categoryRepository.save(Category.builder()
            .code("MARKETING_CONTENT")
            .name("콘텐츠 마케팅")
            .description("콘텐츠 기획 및 제작")
            .categoryType("SUB")
            .parent(marketing)
            .displayOrder(2)
            .isActive(true)
            .build());

        categoryRepository.save(Category.builder()
            .code("DATA_ANALYSIS")
            .name("데이터 분석")
            .description("SQL, Python, 통계")
            .categoryType("SUB")
            .parent(data)
            .displayOrder(1)
            .isActive(true)
            .build());

        categoryRepository.save(Category.builder()
            .code("DATA_ML")
            .name("머신러닝/AI")
            .description("머신러닝 및 딥러닝")
            .categoryType("SUB")
            .parent(data)
            .displayOrder(2)
            .isActive(true)
            .build());
    }
}