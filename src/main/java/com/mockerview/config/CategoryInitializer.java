package com.mockerview.config;

import com.mockerview.entity.Category;
import com.mockerview.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class CategoryInitializer implements CommandLineRunner {

    private final CategoryRepository categoryRepository;

    @Override
    @Transactional
    public void run(String... args) {
        if (categoryRepository.count() > 0) {
            log.info("✅ Category 데이터가 이미 존재합니다. 초기화 스킵.");
            return;
        }

        log.info("🚀 Category 초기 데이터 생성 시작...");

        Category itDev = createMainCategory("IT_DEV", "IT개발", "💻", 1);
        createSubCategories(itDev, Arrays.asList(
            new String[]{"BACKEND", "백엔드", "서버, API, DB 개발"},
            new String[]{"FRONTEND", "프론트엔드", "웹, 모바일 UI 개발"},
            new String[]{"MOBILE", "모바일", "iOS, Android 앱 개발"},
            new String[]{"DATA", "데이터", "데이터 분석, ML/AI"},
            new String[]{"DEVOPS", "DevOps", "인프라, CI/CD, 클라우드"}
        ));

        Category marketing = createMainCategory("MARKETING", "마케팅", "📢", 2);
        createSubCategories(marketing, Arrays.asList(
            new String[]{"DIGITAL", "디지털 마케팅", "SNS, 검색광고, 콘텐츠"},
            new String[]{"CONTENT", "콘텐츠 마케팅", "블로그, 영상, 브랜딩"},
            new String[]{"BRAND", "브랜드 마케팅", "브랜드 전략, 포지셔닝"},
            new String[]{"GROWTH", "그로스 해킹", "데이터 기반 성장 전략"}
        ));

        Category design = createMainCategory("DESIGN", "디자인", "🎨", 3);
        createSubCategories(design, Arrays.asList(
            new String[]{"UIUX", "UI/UX", "사용자 경험, 인터페이스 디자인"},
            new String[]{"GRAPHIC", "그래픽 디자인", "비주얼, 브랜딩 디자인"},
            new String[]{"PRODUCT", "프로덕트 디자인", "제품 기획, 사용성 개선"}
        ));

        Category sales = createMainCategory("SALES", "영업", "💼", 4);
        createSubCategories(sales, Arrays.asList(
            new String[]{"B2B", "B2B 영업", "기업 대상 영업, 제안"},
            new String[]{"B2C", "B2C 영업", "개인 고객 영업, 상담"},
            new String[]{"ACCOUNT", "계정 관리", "기존 고객 관리, 확장"}
        ));

        Category hr = createMainCategory("HR", "인사", "👥", 5);
        createSubCategories(hr, Arrays.asList(
            new String[]{"RECRUIT", "채용", "인재 확보, 면접 진행"},
            new String[]{"HRD", "인재 개발", "교육, 코칭, 성과관리"},
            new String[]{"HRBP", "HR BP", "조직 문화, 제도 운영"}
        ));

        Category finance = createMainCategory("FINANCE", "재무/회계", "💰", 6);
        createSubCategories(finance, Arrays.asList(
            new String[]{"ACCOUNTING", "회계", "재무제표, 세무, 결산"},
            new String[]{"FINANCE", "재무기획", "예산, 투자, 재무전략"}
        ));

        Category gov = createMainCategory("GOV", "공무원", "🏛️", 7);
        createSubCategories(gov, Arrays.asList(
            new String[]{"ADMIN", "행정직", "일반행정, 정책 실행"},
            new String[]{"TECH", "기술직", "전산, 토목, 환경"}
        ));

        Category medical = createMainCategory("MEDICAL", "의료", "🏥", 8);
        createSubCategories(medical, Arrays.asList(
            new String[]{"NURSE", "간호", "환자 케어, 의료 지원"},
            new String[]{"PHARMA", "약사", "조제, 복약 지도"}
        ));

        log.info("✅ Category 초기 데이터 생성 완료! 총 {}개 카테고리", categoryRepository.count());
    }

    private Category createMainCategory(String code, String name, String icon, int order) {
        Category category = Category.builder()
            .code(code)
            .name(name)
            .icon(icon)
            .categoryType("MAIN")
            .displayOrder(order)
            .isActive(true)
            .build();
        return categoryRepository.save(category);
    }

    private void createSubCategories(Category parent, List<String[]> subData) {
        int order = 1;
        for (String[] data : subData) {
            Category sub = Category.builder()
                .code(parent.getCode() + "_" + data[0])
                .name(data[1])
                .description(data[2])
                .categoryType("SUB")
                .parent(parent)
                .displayOrder(order++)
                .isActive(true)
                .build();
            categoryRepository.save(sub);
        }
    }
}
