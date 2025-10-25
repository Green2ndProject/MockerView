window.mbtiData = {
    'ALDS': {
        title: '체계적 분석가',
        subtitle: '데이터 기반 의사결정과 정밀한 실행력을 갖춘 전략적 실무자',
        traits: ['분석적 사고', '논리적 접근', '디테일 중시', '빠른 결정'],
        strengths: [
            { title: '데이터 기반 의사결정', desc: '숫자와 팩트로 말하며, 근거 있는 판단을 내립니다. 감정보다는 객관적 데이터에 의존하여 신뢰성 높은 결정을 합니다.' },
            { title: '체계적 문제 해결', desc: '복잡한 문제를 단계별로 분해하여 해결합니다. 각 요소를 꼼꼼히 분석하고 최적의 솔루션을 찾아냅니다.' },
            { title: '정밀한 실행력', desc: '세부사항까지 놓치지 않고 완벽하게 실행합니다. 계획한 대로 착실히 진행하며 오류를 최소화합니다.' }
        ],
        weaknesses: [
            { title: '창의성 부족', desc: '기존 데이터에 과도하게 의존하여 새로운 시도를 꺼리는 경향이 있습니다. 때로는 직관과 창의적 접근도 필요합니다.' },
            { title: '유연성 필요', desc: '예상치 못한 변수에 대응하는 능력을 키워야 합니다. 계획이 틀어졌을 때도 침착하게 대응하는 연습이 필요합니다.' }
        ],
        careers: [
            { title: '데이터 분석가', desc: '데이터를 수집하고 분석하여 인사이트를 도출합니다.' },
            { title: '재무 분석가', desc: '재무 데이터를 분석하여 투자 결정을 지원합니다.' },
            { title: '프로젝트 매니저', desc: '프로젝트를 체계적으로 관리하고 실행합니다.' },
            { title: '경영 컨설턴트', desc: '기업의 문제를 분석하고 해결책을 제시합니다.' }
        ],
        compatibility: {
            good: [
                { type: 'ALDS', desc: '같은 타입끼리 체계적으로 소통하며 효율적으로 협업합니다.' },
                { type: 'CLBS', desc: '분석력과 창의력이 만나 혁신적인 결과를 만듭니다.' }
            ],
            bad: [
                { type: 'CEBF', desc: '감성적 접근과 논리적 접근의 차이로 갈등이 생길 수 있습니다.' },
                { type: 'CLBF', desc: '자유로운 스타일과 체계적 스타일이 충돌할 수 있습니다.' }
            ]
        },
        tips: [
            '답변 전에 간단한 구조를 머릿속으로 그려보세요 (서론-본론-결론)',
            '구체적인 숫자나 데이터를 활용하면 더 설득력이 있습니다',
            '과거 경험을 STAR 기법으로 정리하여 답변하세요',
            '너무 완벽을 추구하지 말고 80% 완성도에서 답변을 시작하세요',
            '면접관의 질문 의도를 파악한 후 답변의 방향을 정하세요'
        ],
        character: `<svg viewBox="0 0 200 200" xmlns="http://www.w3.org/2000/svg">
            <circle cx="100" cy="100" r="80" fill="#667eea"/>
            <circle cx="75" cy="85" r="8" fill="white"/>
            <circle cx="125" cy="85" r="8" fill="white"/>
            <rect x="75" y="120" width="50" height="6" rx="3" fill="white"/>
            <rect x="50" y="50" width="100" height="80" rx="10" fill="none" stroke="white" stroke-width="3"/>
            <line x1="50" y1="70" x2="150" y2="70" stroke="white" stroke-width="2"/>
            <line x1="70" y1="90" x2="130" y2="90" stroke="white" stroke-width="2"/>
            <line x1="70" y1="105" x2="130" y2="105" stroke="white" stroke-width="2"/>
        </svg>`
    },
    'ALDF': {
        title: '정밀 연구자',
        subtitle: '논리적 분석과 꼼꼼한 검증으로 완벽을 추구하는 전문가',
        traits: ['분석적', '논리적', '완벽주의', '신중함'],
        strengths: [
            { title: '철저한 검증', desc: '모든 것을 꼼꼼히 확인하고 검증합니다. 작은 오류도 놓치지 않는 세심함이 강점입니다.' },
            { title: '논리적 사고', desc: '인과관계를 명확히 파악하고 논리적으로 설명합니다. 체계적인 사고 구조를 가지고 있습니다.' },
            { title: '전문성 추구', desc: '한 분야를 깊이 있게 파고들어 전문가가 됩니다. 지식을 쌓는 것을 즐깁니다.' }
        ],
        weaknesses: [
            { title: '완벽주의', desc: '너무 완벽을 추구하다 진행이 느려질 수 있습니다. 때로는 80%의 완성도로도 충분합니다.' },
            { title: '큰 그림 놓침', desc: '세부사항에 집중하다 전체적인 방향을 놓칠 수 있습니다. 때때로 한 걸음 물러서서 전체를 봐야 합니다.' }
        ],
        careers: [
            { title: '연구원', desc: '특정 분야를 깊이 연구하고 새로운 지식을 창출합니다.' },
            { title: '품질 관리 전문가', desc: '제품과 서비스의 품질을 철저히 관리합니다.' },
            { title: '데이터 사이언티스트', desc: '데이터를 정밀하게 분석하고 인사이트를 도출합니다.' },
            { title: '감사 전문가', desc: '재무제표와 프로세스를 꼼꼼히 검증합니다.' }
        ],
        compatibility: {
            good: [
                { type: 'ALDS', desc: '분석적 사고를 공유하며 깊이 있는 논의가 가능합니다.' },
                { type: 'CLDS', desc: '정밀함과 창의성이 결합되어 훌륭한 결과물을 만듭니다.' }
            ],
            bad: [
                { type: 'CEBS', desc: '빠른 결정과 신중한 검토의 속도 차이로 갈등이 생길 수 있습니다.' },
                { type: 'AEBS', desc: '관계 중심과 논리 중심의 차이로 소통이 어려울 수 있습니다.' }
            ]
        },
        tips: [
            '답변 전에 3초간 생각하고 구조를 잡으세요',
            '완벽한 답변보다는 핵심을 담은 답변이 더 좋습니다',
            '전문용어를 적절히 사용하되, 과도하지 않게 하세요',
            '구체적인 사례와 데이터를 활용하면 설득력이 높아집니다',
            '면접관이 이해했는지 확인하며 답변하세요'
        ],
        character: `<svg viewBox="0 0 200 200" xmlns="http://www.w3.org/2000/svg">
            <circle cx="100" cy="100" r="80" fill="#667eea"/>
            <circle cx="75" cy="85" r="8" fill="white"/>
            <circle cx="125" cy="85" r="8" fill="white"/>
            <path d="M 85 120 Q 100 125 115 120" stroke="white" stroke-width="4" fill="none"/>
            <circle cx="100" cy="60" r="15" fill="none" stroke="white" stroke-width="3"/>
            <line x1="100" y1="60" x2="100" y2="50" stroke="white" stroke-width="3"/>
            <rect x="60" y="140" width="80" height="30" rx="5" fill="white" opacity="0.3"/>
            <line x1="70" y1="148" x2="90" y2="148" stroke="white" stroke-width="2"/>
            <line x1="70" y1="155" x2="85" y2="155" stroke="white" stroke-width="2"/>
            <line x1="70" y1="162" x2="95" y2="162" stroke="white" stroke-width="2"/>
        </svg>`
    },
    'ALBS': {
        title: '전략적 리더',
        subtitle: '큰 그림을 보며 빠르게 결정하는 비전 지향적 의사결정자',
        traits: ['전략적', '결단력', '비전', '리더십'],
        strengths: [
            { title: '전략적 사고', desc: '장기적 관점에서 계획을 수립하고 실행합니다. 미래를 예측하고 대비하는 능력이 뛰어납니다.' },
            { title: '빠른 의사결정', desc: '중요한 순간에 신속하고 단호하게 결정합니다. 우유부단하지 않고 책임감 있게 선택합니다.' },
            { title: '통합적 시각', desc: '여러 요소를 종합하여 전체 그림을 봅니다. 부분이 아닌 전체를 이해하고 방향을 제시합니다.' }
        ],
        weaknesses: [
            { title: '디테일 간과', desc: '큰 그림에 집중하다 세부사항을 놓칠 수 있습니다. 실행 단계에서 예상치 못한 문제가 발생할 수 있습니다.' },
            { title: '성급한 판단', desc: '빠른 결정이 때로는 충분한 검토 없이 이루어질 수 있습니다. 중요한 결정은 한 번 더 검토하세요.' }
        ],
        careers: [
            { title: 'CEO/임원', desc: '조직의 비전을 제시하고 전략적 방향을 결정합니다.' },
            { title: '전략 기획자', desc: '기업의 중장기 전략을 수립하고 실행합니다.' },
            { title: '경영 컨설턴트', desc: '기업의 경영 전략을 분석하고 개선안을 제시합니다.' },
            { title: '사업 개발 리더', desc: '새로운 사업 기회를 발굴하고 추진합니다.' }
        ],
        compatibility: {
            good: [
                { type: 'ALDF', desc: '전략과 실행의 완벽한 조합을 만듭니다.' },
                { type: 'CLBS', desc: '논리와 창의성이 결합되어 혁신적인 전략을 수립합니다.' }
            ],
            bad: [
                { type: 'AEDF', desc: '빠른 결정과 신중한 배려의 속도 차이가 갈등을 만들 수 있습니다.' },
                { type: 'CLBF', desc: '구조화된 접근과 자유로운 접근의 충돌이 있을 수 있습니다.' }
            ]
        },
        tips: [
            '비전과 전략을 명확히 제시하여 리더십을 보여주세요',
            '과거 성과를 구체적인 숫자로 표현하세요',
            '팀을 이끈 경험을 강조하되, 팀원들의 기여도 인정하세요',
            '위기 상황에서의 결단력 있는 대응 사례를 준비하세요',
            '장기적 목표와 단기적 실행 계획을 함께 설명하세요'
        ],
        character: `<svg viewBox="0 0 200 200" xmlns="http://www.w3.org/2000/svg">
            <circle cx="100" cy="100" r="80" fill="#667eea"/>
            <circle cx="75" cy="85" r="8" fill="white"/>
            <circle cx="125" cy="85" r="8" fill="white"/>
            <rect x="75" y="115" width="50" height="8" rx="4" fill="white"/>
            <polygon points="100,40 110,60 90,60" fill="white"/>
            <rect x="85" y="145" width="30" height="35" rx="5" fill="white" opacity="0.3"/>
            <line x1="70" y1="130" x2="130" y2="130" stroke="white" stroke-width="4"/>
            <circle cx="100" cy="40" r="5" fill="white"/>
        </svg>`
    },
    'ALBF': {
        title: '유연한 전략가',
        subtitle: '장기 비전과 적응력을 겸비한 전략적 사고가',
        traits: ['전략적', '유연함', '적응력', '개방적'],
        strengths: [
            { title: '적응력', desc: '변화하는 상황에 빠르게 적응합니다. 새로운 환경에서도 최적의 전략을 찾아냅니다.' },
            { title: '열린 마음', desc: '다양한 의견을 수용하고 통합합니다. 편견 없이 새로운 아이디어를 받아들입니다.' },
            { title: '균형감각', desc: '장기 목표와 현실 상황 사이에서 균형을 찾습니다. 이상과 현실을 조화롭게 추구합니다.' }
        ],
        weaknesses: [
            { title: '우유부단함', desc: '다양한 선택지를 고려하다 결정이 늦어질 수 있습니다. 때로는 과감한 결단이 필요합니다.' },
            { title: '일관성 부족', desc: '상황에 따라 방향이 자주 바뀌면 신뢰를 잃을 수 있습니다. 핵심 원칙은 지켜야 합니다.' }
        ],
        careers: [
            { title: '정책 입안자', desc: '장기적 관점에서 정책을 수립하고 조율합니다.' },
            { title: '교육 기획자', desc: '교육 프로그램을 기획하고 상황에 맞게 조정합니다.' },
            { title: '조직 개발 전문가', desc: '조직의 변화를 이끌고 적응을 지원합니다.' },
            { title: '비즈니스 애널리스트', desc: '비즈니스 전략을 분석하고 개선 방안을 제시합니다.' }
        ],
        compatibility: {
            good: [
                { type: 'CLBF', desc: '창의성과 전략이 만나 혁신적인 아이디어를 실현합니다.' },
                { type: 'AEBF', desc: '논리와 감성의 균형으로 조화로운 협업이 가능합니다.' }
            ],
            bad: [
                { type: 'ALDS', desc: '체계적 접근과 유연한 접근의 차이로 갈등이 생길 수 있습니다.' },
                { type: 'ALBS', desc: '빠른 결정과 신중한 고려의 속도 차이가 문제될 수 있습니다.' }
            ]
        },
        tips: [
            '변화에 대한 적응력을 강조하되, 일관된 가치관도 보여주세요',
            '다양한 관점을 고려한 답변으로 폭넓은 사고를 보여주세요',
            '상황별 대응 전략을 설명하여 유연성을 어필하세요',
            '팀원들의 의견을 통합한 경험을 공유하세요',
            '장기 계획과 단기 조정의 균형을 잡는 방법을 설명하세요'
        ],
        character: `<svg viewBox="0 0 200 200" xmlns="http://www.w3.org/2000/svg">
            <circle cx="100" cy="100" r="80" fill="#667eea"/>
            <circle cx="75" cy="85" r="8" fill="white"/>
            <circle cx="125" cy="85" r="8" fill="white"/>
            <path d="M 80 115 Q 100 125 120 115" stroke="white" stroke-width="4" fill="none"/>
            <path d="M 60,70 Q 100,50 140,70" stroke="white" stroke-width="3" fill="none"/>
            <circle cx="60" cy="70" r="5" fill="white"/>
            <circle cx="140" cy="70" r="5" fill="white"/>
            <circle cx="100" cy="50" r="5" fill="white"/>
        </svg>`
    },
    'AEDS': {
        title: '관계 중심 실행가',
        subtitle: '공감 능력과 실행력으로 팀을 이끄는 리더',
        traits: ['공감적', '실행력', '소통', '결단력'],
        strengths: [
            { title: '뛰어난 공감력', desc: '다른 사람의 입장을 이해하고 배려합니다. 팀원들의 감정을 잘 파악하고 동기부여를 합니다.' },
            { title: '실행력', desc: '계획을 실제로 옮기는 추진력이 있습니다. 말만 하지 않고 직접 행동으로 보여줍니다.' },
            { title: '소통 능력', desc: '명확하고 따뜻하게 의사소통합니다. 팀원들과 신뢰관계를 빠르게 구축합니다.' }
        ],
        weaknesses: [
            { title: '감정적 판단', desc: '때로는 감정이 논리를 앞설 수 있습니다. 중요한 결정은 객관적 근거도 함께 고려해야 합니다.' },
            { title: '과도한 배려', desc: '타인을 배려하다 자신의 의견을 표현하지 못할 수 있습니다. 필요할 때는 단호함도 필요합니다.' }
        ],
        careers: [
            { title: '영업 관리자', desc: '고객과의 관계를 구축하고 팀을 이끌어 목표를 달성합니다.' },
            { title: '마케팅 디렉터', desc: '고객 중심의 마케팅 전략을 수립하고 실행합니다.' },
            { title: '고객 성공 리더', desc: '고객의 성공을 위해 팀을 이끌고 지원합니다.' },
            { title: 'HR 비즈니스 파트너', desc: '직원들을 이해하고 조직 문화를 개선합니다.' }
        ],
        compatibility: {
            good: [
                { type: 'ALDS', desc: '분석력과 공감력이 결합되어 균형 잡힌 팀을 만듭니다.' },
                { type: 'AEBS', desc: '같은 감성적 접근으로 깊은 신뢰 관계를 형성합니다.' }
            ],
            bad: [
                { type: 'CLBS', desc: '논리 중심과 감정 중심의 차이로 소통이 어려울 수 있습니다.' },
                { type: 'ALBS', desc: '빠른 결정과 배려 있는 결정의 속도 차이가 갈등을 만들 수 있습니다.' }
            ]
        },
        tips: [
            '팀워크와 협업 경험을 구체적으로 설명하세요',
            '어려운 상황에서 팀원을 어떻게 도왔는지 사례를 준비하세요',
            '고객이나 동료와의 긍정적인 관계 구축 사례를 공유하세요',
            '감정적 접근과 논리적 분석을 함께 사용한 경험을 강조하세요',
            '갈등 상황을 중재하고 해결한 경험을 설명하세요'
        ],
        character: `<svg viewBox="0 0 200 200" xmlns="http://www.w3.org/2000/svg">
            <circle cx="100" cy="100" r="80" fill="#667eea"/>
            <circle cx="75" cy="85" r="8" fill="white"/>
            <circle cx="125" cy="85" r="8" fill="white"/>
            <path d="M 75 120 Q 100 135 125 120" stroke="white" stroke-width="5" fill="none" stroke-linecap="round"/>
            <path d="M 50,100 L 70,110 L 60,130" stroke="white" stroke-width="4" fill="none" stroke-linecap="round" stroke-linejoin="round"/>
            <path d="M 150,100 L 130,110 L 140,130" stroke="white" stroke-width="4" fill="none" stroke-linecap="round" stroke-linejoin="round"/>
            <circle cx="100" cy="60" r="10" fill="white" opacity="0.3"/>
        </svg>`
    },
    'AEDF': {
        title: '세심한 조력자',
        subtitle: '디테일한 관찰과 진심 어린 지원으로 타인을 돕는 전문가',
        traits: ['세심함', '공감', '배려', '관찰력'],
        strengths: [
            { title: '세심한 관찰', desc: '작은 변화도 놓치지 않고 알아챕니다. 다른 사람의 상태를 빠르게 파악하고 필요한 도움을 제공합니다.' },
            { title: '진심 어린 배려', desc: '타인의 필요를 먼저 생각하고 돕습니다. 진정성 있는 태도로 신뢰를 쌓습니다.' },
            { title: '꼼꼼한 지원', desc: '세부사항까지 챙기며 완벽하게 지원합니다. 누군가를 돕는 일에 책임감을 가집니다.' }
        ],
        weaknesses: [
            { title: '자기주장 부족', desc: '타인을 배려하다 자신의 의견을 말하지 못할 수 있습니다. 때로는 자신의 생각도 표현해야 합니다.' },
            { title: '감정적 소진', desc: '타인의 감정에 과도하게 공감하여 지칠 수 있습니다. 자신을 돌보는 시간도 필요합니다.' }
        ],
        careers: [
            { title: '상담사', desc: '진심으로 경청하고 적절한 조언을 제공합니다.' },
            { title: '사회복지사', desc: '도움이 필요한 사람들을 세심하게 지원합니다.' },
            { title: 'HR 전문가', desc: '직원들의 고민을 듣고 해결책을 찾아줍니다.' },
            { title: '간호사', desc: '환자를 세심하게 관찰하고 돌봅니다.' }
        ],
        compatibility: {
            good: [
                { type: 'AEDS', desc: '공감 능력을 공유하며 서로를 이해하고 지원합니다.' },
                { type: 'CEBF', desc: '감성적 접근과 창의적 접근이 조화를 이룹니다.' }
            ],
            bad: [
                { type: 'ALBS', desc: '빠른 결정과 신중한 배려의 속도 차이가 갈등을 만들 수 있습니다.' },
                { type: 'CLDS', desc: '논리 중심과 감정 중심의 소통 방식 차이가 있을 수 있습니다.' }
            ]
        },
        tips: [
            '타인을 도운 구체적인 사례를 준비하세요',
            '세심한 관찰력으로 문제를 발견한 경험을 공유하세요',
            '감정적 지지와 실질적 도움을 모두 제공한 사례를 설명하세요',
            '어려운 상황에서도 긍정적인 태도를 유지한 경험을 강조하세요',
            '자신의 강점을 표현할 때 겸손하되 명확하게 설명하세요'
        ],
        character: `<svg viewBox="0 0 200 200" xmlns="http://www.w3.org/2000/svg">
            <circle cx="100" cy="100" r="80" fill="#667eea"/>
            <circle cx="75" cy="85" r="8" fill="white"/>
            <circle cx="125" cy="85" r="8" fill="white"/>
            <path d="M 75 115 Q 100 130 125 115" stroke="white" stroke-width="5" fill="none" stroke-linecap="round"/>
            <circle cx="100" cy="60" r="8" fill="white"/>
            <circle cx="80" cy="65" r="4" fill="white"/>
            <circle cx="120" cy="65" r="4" fill="white"/>
            <path d="M 50,140 Q 75,125 100,140 Q 125,155 150,140" stroke="white" stroke-width="3" fill="none"/>
        </svg>`
    },
    'AEBS': {
        title: '비전 관계자',
        subtitle: '사람과 비전을 연결하며 변화를 만드는 리더',
        traits: ['비전', '공감', '추진력', '영감'],
        strengths: [
            { title: '영감을 주는 비전', desc: '사람들에게 희망과 동기를 부여하는 비전을 제시합니다. 함께 가고 싶은 미래를 그립니다.' },
            { title: '관계 구축', desc: '진심으로 사람들과 연결됩니다. 신뢰를 바탕으로 강력한 네트워크를 만듭니다.' },
            { title: '변화 주도', desc: '더 나은 미래를 위해 적극적으로 변화를 이끕니다. 사람들을 설득하고 동참시킵니다.' }
        ],
        weaknesses: [
            { title: '디테일 부족', desc: '큰 그림에 집중하다 실행 세부사항을 놓칠 수 있습니다. 계획을 구체화하는 연습이 필요합니다.' },
            { title: '과도한 낙관', desc: '긍정적인 태도가 때로는 위험을 간과하게 만들 수 있습니다. 현실적인 평가도 필요합니다.' }
        ],
        careers: [
            { title: '사업 개발', desc: '새로운 사업 기회를 발굴하고 관계를 구축합니다.' },
            { title: '영업 전략가', desc: '고객과의 관계를 바탕으로 전략을 수립합니다.' },
            { title: '창업가', desc: '비전을 가지고 사업을 시작하고 성장시킵니다.' },
            { title: '파트너십 매니저', desc: '기업 간 협력 관계를 만들고 관리합니다.' }
        ],
        compatibility: {
            good: [
                { type: 'CEBS', desc: '창의적 비전과 감성적 리더십이 완벽하게 조화됩니다.' },
                { type: 'AEDS', desc: '전략과 실행, 비전과 공감이 균형을 이룹니다.' }
            ],
            bad: [
                { type: 'ALDF', desc: '빠른 추진과 신중한 검토의 속도 차이가 갈등을 만들 수 있습니다.' },
                { type: 'CLDS', desc: '감성적 접근과 논리적 접근의 차이로 소통이 어려울 수 있습니다.' }
            ]
        },
        tips: [
            '비전과 함께 구체적인 실행 계획도 제시하세요',
            '관계 구축을 통해 성과를 낸 사례를 강조하세요',
            '변화를 이끈 리더십 경험을 구체적으로 설명하세요',
            '감성적 호소와 논리적 근거를 균형있게 사용하세요',
            '팀원들을 동기부여한 방법을 공유하세요'
        ],
        character: `<svg viewBox="0 0 200 200" xmlns="http://www.w3.org/2000/svg">
            <circle cx="100" cy="100" r="80" fill="#667eea"/>
            <circle cx="75" cy="85" r="8" fill="white"/>
            <circle cx="125" cy="85" r="8" fill="white"/>
            <path d="M 70 120 Q 100 140 130 120" stroke="white" stroke-width="5" fill="none" stroke-linecap="round"/>
            <polygon points="100,40 115,70 85,70" fill="white"/>
            <circle cx="60" cy="110" r="10" fill="white" opacity="0.3"/>
            <circle cx="140" cy="110" r="10" fill="white" opacity="0.3"/>
        </svg>`
    },
    'AEBF': {
        title: '성장 촉진자',
        subtitle: '유연한 접근으로 조직과 사람의 성장을 돕는 전문가',
        traits: ['성장지향', '유연함', '코칭', '공감'],
        strengths: [
            { title: '성장 마인드', desc: '사람들의 잠재력을 믿고 성장을 지원합니다. 발전 가능성을 발견하고 격려합니다.' },
            { title: '유연한 코칭', desc: '각자의 상황에 맞는 방식으로 코칭합니다. 획일적이지 않고 개별화된 접근을 합니다.' },
            { title: '조직 문화 개선', desc: '건강한 조직 문화를 만드는 데 기여합니다. 사람들이 성장할 수 있는 환경을 조성합니다.' }
        ],
        weaknesses: [
            { title: '명확한 기준 부족', desc: '유연한 접근이 때로는 일관성 없어 보일 수 있습니다. 핵심 원칙은 명확히 해야 합니다.' },
            { title: '느린 의사결정', desc: '모든 사람을 고려하다 결정이 늦어질 수 있습니다. 때로는 과감한 선택도 필요합니다.' }
        ],
        careers: [
            { title: 'HR 매니저', desc: '직원들의 성장과 발전을 지원합니다.' },
            { title: '조직문화 전문가', desc: '건강한 조직 문화를 만들고 개선합니다.' },
            { title: '리더십 코치', desc: '리더들의 성장을 돕는 코칭을 제공합니다.' },
            { title: '인재 개발 담당자', desc: '직원들의 역량 개발 프로그램을 기획합니다.' }
        ],
        compatibility: {
            good: [
                { type: 'ALBF', desc: '전략적 유연성과 인간적 접근이 조화를 이룹니다.' },
                { type: 'CEBF', desc: '창의적 성장과 감성적 성장이 시너지를 냅니다.' }
            ],
            bad: [
                { type: 'ALDS', desc: '체계적 접근과 유연한 접근의 차이로 갈등이 생길 수 있습니다.' },
                { type: 'ALBS', desc: '빠른 결정과 신중한 고려의 속도 차이가 문제될 수 있습니다.' }
            ]
        },
        tips: [
            '사람들의 성장을 도운 구체적인 사례를 준비하세요',
            '조직 문화 개선에 기여한 경험을 공유하세요',
            '상황에 맞는 유연한 대응 능력을 보여주세요',
            '코칭이나 멘토링 경험을 구체적으로 설명하세요',
            '장기적 관점의 인재 육성 계획을 제시하세요'
        ],
        character: `<svg viewBox="0 0 200 200" xmlns="http://www.w3.org/2000/svg">
            <circle cx="100" cy="100" r="80" fill="#667eea"/>
            <circle cx="75" cy="85" r="8" fill="white"/>
            <circle cx="125" cy="85" r="8" fill="white"/>
            <path d="M 75 115 Q 100 130 125 115" stroke="white" stroke-width="5" fill="none" stroke-linecap="round"/>
            <path d="M 70,50 Q 100,35 130,50" stroke="white" stroke-width="3" fill="none"/>
            <circle cx="70" cy="50" r="6" fill="white"/>
            <circle cx="100" cy="35" r="6" fill="white"/>
            <circle cx="130" cy="50" r="6" fill="white"/>
            <path d="M 50,130 Q 100,145 150,130" stroke="white" stroke-width="3" fill="none" opacity="0.5"/>
        </svg>`
    },
    'CLDS': {
        title: '창의적 엔지니어',
        subtitle: '혁신적 아이디어를 정밀하게 구현하는 기술 전문가',
        traits: ['창의적', '논리적', '정밀함', '혁신'],
        strengths: [
            { title: '창의적 문제해결', desc: '기존과 다른 방식으로 문제를 해결합니다. 독창적인 접근으로 혁신적인 솔루션을 만듭니다.' },
            { title: '기술적 정밀성', desc: '아이디어를 정확하게 구현합니다. 창의성과 기술력을 동시에 발휘합니다.' },
            { title: '혁신 추구', desc: '새로운 기술과 방법론을 끊임없이 탐구합니다. 최신 트렌드를 빠르게 습득합니다.' }
        ],
        weaknesses: [
            { title: '완벽주의', desc: '창의적 완성도를 추구하다 시간이 오래 걸릴 수 있습니다. 때로는 빠른 프로토타입도 필요합니다.' },
            { title: '소통 부족', desc: '기술에 집중하다 팀원들과의 소통을 소홀히 할 수 있습니다. 설명과 공유가 필요합니다.' }
        ],
        careers: [
            { title: '소프트웨어 개발자', desc: '혁신적인 소프트웨어를 설계하고 개발합니다.' },
            { title: '엔지니어', desc: '창의적인 기술 솔루션을 구현합니다.' },
            { title: '시스템 설계자', desc: '복잡한 시스템을 설계하고 최적화합니다.' },
            { title: '프로덕트 개발자', desc: '사용자 경험을 개선하는 제품을 만듭니다.' }
        ],
        compatibility: {
            good: [
                { type: 'ALDF', desc: '창의성과 정밀함이 만나 완벽한 결과물을 만듭니다.' },
                { type: 'CLBS', desc: '혁신적 아이디어가 시너지를 내며 발전합니다.' }
            ],
            bad: [
                { type: 'AEBS', desc: '기술 중심과 관계 중심의 우선순위 차이가 있을 수 있습니다.' },
                { type: 'CEBF', desc: '논리적 접근과 감성적 접근의 차이로 갈등이 생길 수 있습니다.' }
            ]
        },
        tips: [
            '기술적 혁신 사례를 구체적으로 설명하세요',
            '복잡한 문제를 창의적으로 해결한 경험을 공유하세요',
            '기술 트렌드에 대한 관심과 학습 능력을 보여주세요',
            '팀 프로젝트에서의 기여도를 명확히 설명하세요',
            '기술을 비전문가에게도 쉽게 설명할 수 있음을 보여주세요'
        ],
        character: `<svg viewBox="0 0 200 200" xmlns="http://www.w3.org/2000/svg">
            <circle cx="100" cy="100" r="80" fill="#667eea"/>
            <circle cx="75" cy="85" r="8" fill="white"/>
            <circle cx="125" cy="85" r="8" fill="white"/>
            <rect x="85" y="115" width="30" height="10" rx="2" fill="white"/>
            <polygon points="100,45 115,60 100,75 85,60" fill="white" opacity="0.3"/>
            <circle cx="60" cy="130" r="8" fill="white" opacity="0.5"/>
            <circle cx="140" cy="130" r="8" fill="white" opacity="0.5"/>
            <line x1="60" y1="130" x2="80" y2="110" stroke="white" stroke-width="2"/>
            <line x1="140" y1="130" x2="120" y2="110" stroke="white" stroke-width="2"/>
        </svg>`
    },
    'CLDF': {
        title: '완벽주의 디자이너',
        subtitle: '창의성과 디테일의 조화로 완성도 높은 결과물 창출',
        traits: ['창의적', '완벽주의', '디테일', '미학'],
        strengths: [
            { title: '미적 감각', desc: '아름답고 조화로운 결과물을 만듭니다. 디테일까지 신경 써서 완성도를 높입니다.' },
            { title: '창의적 정밀성', desc: '창의적이면서도 정확합니다. 예술성과 기능성을 모두 갖춘 작품을 만듭니다.' },
            { title: '품질 추구', desc: '최고의 품질을 위해 노력합니다. 타협하지 않고 최선을 다합니다.' }
        ],
        weaknesses: [
            { title: '과도한 완벽주의', desc: '너무 완벽을 추구하다 일정이 지연될 수 있습니다. 적절한 선에서 마무리하는 것도 중요합니다.' },
            { title: '비판에 민감', desc: '작품에 대한 비판을 개인적으로 받아들일 수 있습니다. 피드백을 성장의 기회로 삼아야 합니다.' }
        ],
        careers: [
            { title: 'UX 디자이너', desc: '사용자 경험을 디자인하고 개선합니다.' },
            { title: '제품 디자이너', desc: '아름답고 기능적인 제품을 디자인합니다.' },
            { title: '건축가', desc: '창의적이고 정밀한 건축물을 설계합니다.' },
            { title: '그래픽 디자이너', desc: '시각적으로 완성도 높은 디자인을 만듭니다.' }
        ],
        compatibility: {
            good: [
                { type: 'CLDS', desc: '창의성과 기술력이 만나 혁신적인 디자인을 만듭니다.' },
                { type: 'ALDF', desc: '논리와 미학이 조화되어 완벽한 결과물을 만듭니다.' }
            ],
            bad: [
                { type: 'AEBS', desc: '완벽 추구와 빠른 실행의 속도 차이가 갈등을 만들 수 있습니다.' },
                { type: 'CLBF', desc: '체계적 접근과 자유로운 창작의 충돌이 있을 수 있습니다.' }
            ]
        },
        tips: [
            '포트폴리오를 준비하여 실제 작품을 보여주세요',
            '창의적 과정과 디테일한 실행을 모두 설명하세요',
            '피드백을 받고 개선한 사례를 공유하세요',
            '일정 내에 완성한 프로젝트 경험을 강조하세요',
            '사용자나 클라이언트의 반응을 구체적으로 설명하세요'
        ],
        character: `<svg viewBox="0 0 200 200" xmlns="http://www.w3.org/2000/svg">
            <circle cx="100" cy="100" r="80" fill="#667eea"/>
            <circle cx="75" cy="85" r="8" fill="white"/>
            <circle cx="125" cy="85" r="8" fill="white"/>
            <path d="M 80 115 Q 100 125 120 115" stroke="white" stroke-width="4" fill="none"/>
            <rect x="70" y="50" width="60" height="20" rx="10" fill="white" opacity="0.3"/>
            <circle cx="60" cy="140" r="10" fill="white" opacity="0.4"/>
            <circle cx="100" cy="145" r="8" fill="white" opacity="0.5"/>
            <circle cx="140" cy="140" r="10" fill="white" opacity="0.4"/>
        </svg>`
    },
    'CLBS': {
        title: '혁신 비전가',
        subtitle: '창의적 아이디어로 새로운 미래를 제시하는 변화 주도자',
        traits: ['혁신', '비전', '창의적', '리더십'],
        strengths: [
            { title: '혁신적 사고', desc: '기존의 틀을 깨고 새로운 가능성을 제시합니다. 미래를 예측하고 선도합니다.' },
            { title: '영감 제공', desc: '사람들에게 영감을 주는 비전을 만듭니다. 함께 꿈꾸고 실현하도록 이끕니다.' },
            { title: '변화 주도', desc: '현재에 안주하지 않고 끊임없이 변화를 추구합니다. 더 나은 미래를 만들기 위해 노력합니다.' }
        ],
        weaknesses: [
            { title: '실행력 부족', desc: '아이디어는 많지만 실행이 부족할 수 있습니다. 구체적인 계획과 실행이 필요합니다.' },
            { title: '현실성 부족', desc: '이상적인 비전을 추구하다 현실을 간과할 수 있습니다. 실현 가능성도 고려해야 합니다.' }
        ],
        careers: [
            { title: '크리에이티브 디렉터', desc: '창의적 방향을 제시하고 팀을 이끕니다.' },
            { title: '브랜드 전략가', desc: '브랜드의 미래를 설계하고 전략을 수립합니다.' },
            { title: '혁신 리더', desc: '조직의 혁신을 주도하고 문화를 바꿉니다.' },
            { title: '비즈니스 혁신가', desc: '새로운 비즈니스 모델을 만들고 실험합니다.' }
        ],
        compatibility: {
            good: [
                { type: 'ALDS', desc: '창의적 비전과 체계적 실행이 완벽한 조합을 만듭니다.' },
                { type: 'CLBS', desc: '혁신적 아이디어가 시너지를 내며 확장됩니다.' }
            ],
            bad: [
                { type: 'ALDF', desc: '혁신과 안정, 변화와 검증의 우선순위 차이가 갈등을 만들 수 있습니다.' },
                { type: 'AEDF', desc: '빠른 변화와 신중한 배려의 속도 차이가 문제될 수 있습니다.' }
            ]
        },
        tips: [
            '혁신적인 아이디어와 함께 실행 계획도 제시하세요',
            '변화를 이끈 구체적인 사례를 준비하세요',
            '비전을 현실로 만든 경험을 강조하세요',
            '트렌드를 읽고 선도한 사례를 공유하세요',
            '팀을 설득하고 동참시킨 방법을 설명하세요'
        ],
        character: `<svg viewBox="0 0 200 200" xmlns="http://www.w3.org/2000/svg">
            <circle cx="100" cy="100" r="80" fill="#667eea"/>
            <circle cx="75" cy="85" r="8" fill="white"/>
            <circle cx="125" cy="85" r="8" fill="white"/>
            <rect x="75" y="115" width="50" height="8" rx="4" fill="white"/>
            <polygon points="100,35 120,55 80,55" fill="white"/>
            <circle cx="100" cy="35" r="8" fill="white"/>
            <path d="M 50,130 L 70,145" stroke="white" stroke-width="4"/>
            <path d="M 150,130 L 130,145" stroke="white" stroke-width="4"/>
            <circle cx="50" cy="130" r="6" fill="white"/>
            <circle cx="150" cy="130" r="6" fill="white"/>
        </svg>`
    },
    'CLBF': {
        title: '자유로운 창작자',
        subtitle: '제약 없이 새로운 것을 탐구하고 창조하는 아티스트',
        traits: ['자유로움', '창의적', '독창적', '실험적'],
        strengths: [
            { title: '무한한 창의성', desc: '틀에 얽매이지 않고 자유롭게 창작합니다. 독특하고 개성 있는 작품을 만듭니다.' },
            { title: '실험 정신', desc: '새로운 시도를 두려워하지 않습니다. 실패를 두려워하지 않고 계속 도전합니다.' },
            { title: '독창성', desc: '자신만의 스타일과 방식을 가지고 있습니다. 남들과 다른 독특한 접근을 합니다.' }
        ],
        weaknesses: [
            { title: '체계 부족', desc: '자유로운 스타일로 인해 체계적이지 못할 수 있습니다. 때로는 구조화된 접근도 필요합니다.' },
            { title: '마감 지연', desc: '완벽한 영감을 기다리다 마감을 놓칠 수 있습니다. 일정 관리가 중요합니다.' }
        ],
        careers: [
            { title: '아티스트', desc: '자유롭게 예술 작품을 창작합니다.' },
            { title: '작가', desc: '독창적인 콘텐츠와 스토리를 만듭니다.' },
            { title: '디자인 씽커', desc: '혁신적인 디자인 솔루션을 탐구합니다.' },
            { title: '크리에이터', desc: '새로운 형태의 콘텐츠를 실험하고 만듭니다.' }
        ],
        compatibility: {
            good: [
                { type: 'CLBF', desc: '자유로운 창작자끼리 서로를 이해하고 영감을 줍니다.' },
                { type: 'CEBF', desc: '창의성과 감성이 만나 아름다운 작품을 만듭니다.' }
            ],
            bad: [
                { type: 'ALDS', desc: '자유로운 스타일과 체계적 스타일이 충돌할 수 있습니다.' },
                { type: 'ALBS', desc: '즉흥적 창작과 계획적 실행의 차이가 갈등을 만들 수 있습니다.' }
            ]
        },
        tips: [
            '독창적인 프로젝트나 작품을 포트폴리오로 준비하세요',
            '창작 과정의 자유로움과 결과물의 완성도를 모두 강조하세요',
            '실험적 시도가 성공한 사례를 공유하세요',
            '일정 내에 완성한 경험으로 책임감을 보여주세요',
            '팀 프로젝트에서 창의성으로 기여한 점을 설명하세요'
        ],
        character: `<svg viewBox="0 0 200 200" xmlns="http://www.w3.org/2000/svg">
            <circle cx="100" cy="100" r="80" fill="#667eea"/>
            <circle cx="75" cy="85" r="8" fill="white"/>
            <circle cx="125" cy="85" r="8" fill="white"/>
            <path d="M 75 120 Q 100 130 125 120" stroke="white" stroke-width="5" fill="none" stroke-linecap="round"/>
            <path d="M 60,60 Q 80,40 100,50 Q 120,40 140,60" stroke="white" stroke-width="3" fill="none"/>
            <circle cx="60" cy="60" r="5" fill="white"/>
            <circle cx="100" cy="50" r="5" fill="white"/>
            <circle cx="140" cy="60" r="5" fill="white"/>
            <path d="M 50,140 Q 100,160 150,140" stroke="white" stroke-width="3" fill="none" opacity="0.5"/>
        </svg>`
    },
    'CEDS': {
        title: '역동적 혁신가',
        subtitle: '빠른 실행으로 창의적 아이디어를 현실로 만드는 실행가',
        traits: ['역동적', '창의적', '실행력', '열정'],
        strengths: [
            { title: '빠른 실행', desc: '아이디어를 즉시 실행에 옮깁니다. 생각과 행동 사이의 간격이 짧습니다.' },
            { title: '열정적 추진', desc: '에너지 넘치게 프로젝트를 추진합니다. 주변 사람들에게도 열정을 전파합니다.' },
            { title: '창의적 실험', desc: '새로운 것을 시도하고 빠르게 테스트합니다. 실패를 두려워하지 않고 계속 도전합니다.' }
        ],
        weaknesses: [
            { title: '성급함', desc: '너무 빨리 진행하다 중요한 부분을 놓칠 수 있습니다. 때로는 신중한 검토도 필요합니다.' },
            { title: '지속성 부족', desc: '새로운 아이디어에 쉽게 흥미를 느끼다 기존 프로젝트를 마무리하지 못할 수 있습니다.' }
        ],
        careers: [
            { title: '마케터', desc: '창의적인 마케팅 캠페인을 빠르게 실행합니다.' },
            { title: '콘텐츠 크리에이터', desc: '다양한 콘텐츠를 만들고 빠르게 배포합니다.' },
            { title: '스타트업 창업자', desc: '아이디어를 빠르게 사업화하고 성장시킵니다.' },
            { title: '프로덕트 매니저', desc: '빠르게 제품을 출시하고 개선합니다.' }
        ],
        compatibility: {
            good: [
                { type: 'AEBS', desc: '창의적 실행과 관계 구축이 완벽하게 조화됩니다.' },
                { type: 'CEDS', desc: '서로의 에너지와 추진력이 시너지를 냅니다.' }
            ],
            bad: [
                { type: 'ALDF', desc: '빠른 실행과 신중한 검토의 속도 차이가 갈등을 만들 수 있습니다.' },
                { type: 'AEDF', desc: '역동적 추진과 세심한 배려의 차이로 충돌할 수 있습니다.' }
            ]
        },
        tips: [
            '빠르게 실행하고 성과를 낸 사례를 강조하세요',
            '창의적 아이디어를 현실로 만든 과정을 설명하세요',
            '실패를 극복하고 개선한 경험을 공유하세요',
            '열정과 함께 책임감도 보여주세요',
            '팀을 동기부여하고 이끈 리더십을 설명하세요'
        ],
        character: `<svg viewBox="0 0 200 200" xmlns="http://www.w3.org/2000/svg">
            <circle cx="100" cy="100" r="80" fill="#667eea"/>
            <circle cx="75" cy="85" r="8" fill="white"/>
            <circle cx="125" cy="85" r="8" fill="white"/>
            <path d="M 70 120 Q 100 135 130 120" stroke="white" stroke-width="5" fill="none" stroke-linecap="round"/>
            <polygon points="100,40 110,65 90,65" fill="white"/>
            <line x1="50" y1="100" x2="70" y2="110" stroke="white" stroke-width="4" stroke-linecap="round"/>
            <line x1="150" y1="100" x2="130" y2="110" stroke="white" stroke-width="4" stroke-linecap="round"/>
            <circle cx="50" cy="100" r="6" fill="white"/>
            <circle cx="150" cy="100" r="6" fill="white"/>
        </svg>`
    },
    'CEDF': {
        title: '감성 창작자',
        subtitle: '감성과 창의성으로 사람들에게 영감을 주는 예술가',
        traits: ['감성적', '창의적', '공감', '예술적'],
        strengths: [
            { title: '깊은 감성', desc: '감정을 잘 표현하고 다른 사람의 감정도 이해합니다. 진정성 있는 작품을 만듭니다.' },
            { title: '예술적 재능', desc: '아름다움을 창조하는 능력이 뛰어납니다. 사람들의 마음을 움직이는 작품을 만듭니다.' },
            { title: '치유 능력', desc: '예술로 사람들을 위로하고 치유합니다. 긍정적인 영향을 주는 작업을 합니다.' }
        ],
        weaknesses: [
            { title: '감정 기복', desc: '감정에 따라 컨디션이 크게 변할 수 있습니다. 감정 관리가 중요합니다.' },
            { title: '실용성 부족', desc: '예술성을 추구하다 실용적 측면을 간과할 수 있습니다. 현실적 고려도 필요합니다.' }
        ],
        careers: [
            { title: '예술 치료사', desc: '예술로 사람들의 마음을 치유합니다.' },
            { title: '심리 상담사', desc: '감성적 접근으로 상담과 치료를 제공합니다.' },
            { title: '창의적 교육자', desc: '창의성을 키우는 교육을 제공합니다.' },
            { title: '작가/아티스트', desc: '감성적이고 창의적인 작품을 만듭니다.' }
        ],
        compatibility: {
            good: [
                { type: 'AEDF', desc: '깊은 공감과 감성적 교류로 서로를 이해합니다.' },
                { type: 'CLBF', desc: '창의성과 감성이 만나 아름다운 작품을 만듭니다.' }
            ],
            bad: [
                { type: 'ALDS', desc: '감성적 접근과 논리적 접근의 차이로 소통이 어려울 수 있습니다.' },
                { type: 'ALBS', desc: '빠른 결정과 감성적 고려의 충돌이 있을 수 있습니다.' }
            ]
        },
        tips: [
            '감성적인 작품이나 프로젝트 사례를 준비하세요',
            '사람들에게 긍정적 영향을 준 경험을 공유하세요',
            '창의적 과정에서의 감정과 영감을 설명하세요',
            '감성과 함께 전문성도 강조하세요',
            '피드백을 받고 성장한 과정을 보여주세요'
        ],
        character: `<svg viewBox="0 0 200 200" xmlns="http://www.w3.org/2000/svg">
            <circle cx="100" cy="100" r="80" fill="#667eea"/>
            <circle cx="75" cy="85" r="8" fill="white"/>
            <circle cx="125" cy="85" r="8" fill="white"/>
            <path d="M 75 115 Q 100 130 125 115" stroke="white" stroke-width="5" fill="none" stroke-linecap="round"/>
            <path d="M 60,60 L 70,70 L 60,80" stroke="white" stroke-width="3" fill="none" stroke-linecap="round" stroke-linejoin="round"/>
            <path d="M 140,60 L 130,70 L 140,80" stroke="white" stroke-width="3" fill="none" stroke-linecap="round" stroke-linejoin="round"/>
            <circle cx="100" cy="50" r="10" fill="white" opacity="0.3"/>
            <path d="M 50,140 Q 100,155 150,140" stroke="white" stroke-width="3" fill="none" opacity="0.5"/>
        </svg>`
    },
    'CEBS': {
        title: '변화 선도자',
        subtitle: '사회적 가치와 혁신으로 세상을 변화시키는 리더',
        traits: ['혁신', '공감', '리더십', '사회적'],
        strengths: [
            { title: '사회적 영향력', desc: '더 나은 세상을 만들기 위해 노력합니다. 사회적 가치를 추구하며 변화를 이끕니다.' },
            { title: '공감 기반 혁신', desc: '사람들의 필요를 이해하고 혁신적 솔루션을 만듭니다. 사용자 중심의 변화를 추구합니다.' },
            { title: '영감 제공', desc: '사람들에게 희망과 동기를 부여합니다. 함께 변화를 만들어가도록 이끕니다.' }
        ],
        weaknesses: [
            { title: '이상주의', desc: '이상을 추구하다 현실적 제약을 간과할 수 있습니다. 실현 가능성도 고려해야 합니다.' },
            { title: '소진 위험', desc: '과도한 헌신으로 지칠 수 있습니다. 자신을 돌보는 시간도 필요합니다.' }
        ],
        careers: [
            { title: '소셜 벤처 창업자', desc: '사회 문제를 해결하는 비즈니스를 만듭니다.' },
            { title: '비전 리더', desc: '조직의 사회적 가치를 만들고 실현합니다.' },
            { title: '혁신 담당자', desc: '사회적 혁신을 주도하고 문화를 바꿉니다.' },
            { title: 'CSR 리더', desc: '기업의 사회적 책임 활동을 기획하고 실행합니다.' }
        ],
        compatibility: {
            good: [
                { type: 'AEBS', desc: '사회적 가치와 관계 구축이 완벽하게 조화됩니다.' },
                { type: 'CEBS', desc: '같은 가치를 공유하며 큰 변화를 만들어갑니다.' }
            ],
            bad: [
                { type: 'ALDS', desc: '이상 추구와 현실 중심의 우선순위 차이가 갈등을 만들 수 있습니다.' },
                { type: 'CLDF', desc: '빠른 변화와 완벽한 준비의 속도 차이가 문제될 수 있습니다.' }
            ]
        },
        tips: [
            '사회적 가치를 창출한 프로젝트를 강조하세요',
            '변화를 이끈 리더십 경험을 구체적으로 설명하세요',
            '팀을 동기부여하고 영감을 준 방법을 공유하세요',
            '이상과 현실의 균형을 맞춘 경험을 보여주세요',
            '지속 가능한 변화를 만든 사례를 설명하세요'
        ],
        character: `<svg viewBox="0 0 200 200" xmlns="http://www.w3.org/2000/svg">
            <circle cx="100" cy="100" r="80" fill="#667eea"/>
            <circle cx="75" cy="85" r="8" fill="white"/>
            <circle cx="125" cy="85" r="8" fill="white"/>
            <path d="M 70 120 Q 100 140 130 120" stroke="white" stroke-width="5" fill="none" stroke-linecap="round"/>
            <polygon points="100,35 115,60 85,60" fill="white"/>
            <circle cx="100" cy="35" r="8" fill="white"/>
            <circle cx="60" cy="110" r="12" fill="white" opacity="0.3"/>
            <circle cx="140" cy="110" r="12" fill="white" opacity="0.3"/>
            <circle cx="100" cy="145" r="8" fill="white" opacity="0.4"/>
        </svg>`
    },
    'CEBF': {
        title: '조화로운 혁신가',
        subtitle: '공감과 변화를 균형있게 이끄는 포용적 리더',
        traits: ['조화', '혁신', '포용', '유연함'],
        strengths: [
            { title: '포용적 리더십', desc: '모든 사람을 포용하며 함께 성장합니다. 다양성을 존중하고 활용합니다.' },
            { title: '균형감각', desc: '변화와 안정, 혁신과 전통의 균형을 잡습니다. 극단을 피하고 조화를 추구합니다.' },
            { title: '유연한 혁신', desc: '상황에 맞게 변화를 이끕니다. 강압적이지 않고 자연스럽게 변화를 만듭니다.' }
        ],
        weaknesses: [
            { title: '우유부단함', desc: '모두를 고려하다 결정이 늦어질 수 있습니다. 때로는 과감한 선택도 필요합니다.' },
            { title: '과도한 타협', desc: '조화를 추구하다 핵심 가치를 타협할 수 있습니다. 중요한 원칙은 지켜야 합니다.' }
        ],
        careers: [
            { title: '비영리 활동가', desc: '포용적 방식으로 사회 문제를 해결합니다.' },
            { title: '변화 관리자', desc: '조직의 변화를 유연하게 이끌고 관리합니다.' },
            { title: '다양성 전문가', desc: '조직의 다양성과 포용성을 증진합니다.' },
            { title: '갈등 중재자', desc: '다양한 이해관계자 간의 조화를 만듭니다.' }
        ],
        compatibility: {
            good: [
                { type: 'AEBF', desc: '포용적 접근과 성장 마인드가 완벽하게 조화됩니다.' },
                { type: 'CEBF', desc: '같은 가치를 공유하며 조화로운 변화를 만듭니다.' }
            ],
            bad: [
                { type: 'ALBS', desc: '빠른 결정과 포용적 고려의 속도 차이가 갈등을 만들 수 있습니다.' },
                { type: 'CEDS', desc: '역동적 추진과 조화로운 진행의 속도 차이가 문제될 수 있습니다.' }
            ]
        },
        tips: [
            '다양한 이해관계자를 조율한 경험을 강조하세요',
            '갈등을 해결하고 조화를 만든 사례를 공유하세요',
            '포용적 리더십으로 팀을 이끈 경험을 설명하세요',
            '유연하면서도 원칙을 지킨 사례를 보여주세요',
            '지속 가능한 변화를 만든 방법을 설명하세요'
        ],
        character: `<svg viewBox="0 0 200 200" xmlns="http://www.w3.org/2000/svg">
            <circle cx="100" cy="100" r="80" fill="#667eea"/>
            <circle cx="75" cy="85" r="8" fill="white"/>
            <circle cx="125" cy="85" r="8" fill="white"/>
            <path d="M 75 115 Q 100 130 125 115" stroke="white" stroke-width="5" fill="none" stroke-linecap="round"/>
            <circle cx="60" cy="70" r="8" fill="white" opacity="0.5"/>
            <circle cx="140" cy="70" r="8" fill="white" opacity="0.5"/>
            <circle cx="100" cy="50" r="8" fill="white" opacity="0.5"/>
            <circle cx="60" cy="130" r="8" fill="white" opacity="0.4"/>
            <circle cx="140" cy="130" r="8" fill="white" opacity="0.4"/>
            <path d="M 60,70 Q 100,50 140,70" stroke="white" stroke-width="2" fill="none" opacity="0.5"/>
        </svg>`
    }
};
