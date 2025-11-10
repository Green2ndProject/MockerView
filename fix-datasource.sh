#!/bin/bash
perl -i.bak -pe '
s|jdbc:postgresql://localhost:5433/mockerview_pro|jdbc:postgresql://localhost:5432/mockerview_dev|g;
s/mockerview_pro_user/mockerview_dev_user/g;
s/mockerview_pro_pass/mockerview_dev_pass/g;
' application.yml
echo "✅ 수정 완료!"
grep -A5 "url: jdbc:postgresql" application.yml | head -6
