document.addEventListener('DOMContentLoaded', function() {
    if (typeof sessionId !== 'undefined') {
        loadAnswers();
    }
    
    const modal = document.getElementById('reviewModal');
    const closeBtn = document.querySelector('.close');
    
    closeBtn?.addEventListener('click', () => {
        modal.style.display = 'none';
    });
    
    window.addEventListener('click', (e) => {
        if (e.target === modal) {
            modal.style.display = 'none';
        }
    });
    
    document.getElementById('reviewForm')?.addEventListener('submit', handleReviewSubmit);
});

let currentAnswerId = null;

async function loadAnswers() {
    try {
        const sessionResponse = await authFetch(`/api/sessions/${sessionId}`);
        const session = await sessionResponse.json();
        document.getElementById('sessionTitle').textContent = `${session.title} - 리뷰`;
        
        const answersResponse = await authFetch(`/api/sessions/${sessionId}/answers`);
        const answers = await answersResponse.json();
        
        const answersList = document.getElementById('answersList');
        answersList.innerHTML = '';
        
        for (const answer of answers) {
            const card = document.createElement('div');
            card.className = 'answer-card';
            card.innerHTML = `
                <h3>질문: ${answer.question.text}</h3>
                <p><strong>답변자:</strong> ${answer.user.name}</p>
                <p class="answer-text">${answer.text}</p>
                <p><strong>점수:</strong> ${answer.score || '미평가'}</p>
                <button class="btn-primary" onclick="openReviewModal(${answer.id}, '${answer.question.text}', '${answer.text}')">리뷰 작성</button>
                <div id="reviews-${answer.id}"></div>
            `;
            answersList.appendChild(card);
            
            loadReviews(answer.id);
        }
    } catch (error) {
        console.error('Error:', error);
        alert('답변 로드 실패');
    }
}

async function loadReviews(answerId) {
    try {
        const response = await authFetch(`/api/review/answer/${answerId}`);
        const reviews = await response.json();
        
        const reviewsDiv = document.getElementById(`reviews-${answerId}`);
        if (reviews.length > 0) {
            reviewsDiv.innerHTML = '<h4>기존 리뷰</h4>';
            reviews.forEach(review => {
                reviewsDiv.innerHTML += `
                    <div class="review-item">
                        <p><strong>${review.reviewerName}</strong> - 평점: ${review.rating}</p>
                        <p>${review.reviewComment}</p>
                    </div>
                `;
            });
        }
    } catch (error) {
        console.error('Error loading reviews:', error);
    }
}

function openReviewModal(answerId, questionText, answerText) {
    currentAnswerId = answerId;
    document.getElementById('answerPreview').innerHTML = `
        <p><strong>질문:</strong> ${questionText}</p>
        <p><strong>답변:</strong> ${answerText}</p>
    `;
    document.getElementById('reviewModal').style.display = 'block';
    document.getElementById('reviewForm').reset();
}

async function handleReviewSubmit(e) {
    e.preventDefault();
    
    const rating = document.getElementById('rating').value;
    const comment = document.getElementById('reviewComment').value;
    
    const data = {
        sessionId: sessionId,
        answerId: currentAnswerId,
        rating: parseFloat(rating),
        comment: comment
    };
    
    console.log('Sending review data:', data);
    
    try {
        const response = await authPost('/api/review', data);
        
        if (response.ok) {
            alert('리뷰가 등록되었습니다');
            document.getElementById('reviewModal').style.display = 'none';
            loadAnswers();
        } else {
            const errorText = await response.text();
            console.error('Server error:', errorText);
            alert('리뷰 등록 실패');
        }
    } catch (error) {
        console.error('Error:', error);
        alert('오류가 발생했습니다');
    }
}