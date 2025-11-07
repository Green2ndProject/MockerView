#!/bin/bash

# styles.html의 footer 부분만 수정
sed -i '' '/.footer {/,/}/c\
    .footer { \
        background: #000000; \
        background: linear-gradient(180deg, #0a0a0a 0%, #000000 100%); \
        color: #ffffff; \
        padding: 48px 2rem; \
        text-align: center; \
        border-top: 1px solid #1a1a1a; \
    }' src/main/resources/templates/fragments/styles.html

sed -i '' '/.footer p {/,/}/c\
    .footer p { \
        font-size: 0.875rem; \
        font-weight: 400; \
        color: #ffffff; \
        margin: 0; \
        letter-spacing: 0.5px; \
        opacity: 0.9; \
    }' src/main/resources/templates/fragments/styles.html

echo "✅ Footer 스타일 수정 완료!"
echo "  - 배경: 검은색"
echo "  - 글자: 흰색"
echo "  - 정렬: 가운데"
