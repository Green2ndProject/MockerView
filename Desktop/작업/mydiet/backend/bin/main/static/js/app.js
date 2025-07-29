const API_BASE = '/api';
const CURRENT_USER_ID = 1;

document.addEventListener('DOMContentLoaded', function() {
    loadTodayData();
    setupEventListeners();
});

function setupEventListeners() {
    document.getElementById('meal-form').addEventListener('submit', handleMealSubmit);
    document.getElementById('workout-form').addEventListener('submit', handleWorkoutSubmit);
    document.getElementById('emotion-form').addEventListener('submit', handleEmotionSubmit);
    
    window.addEventListener('click', function(event) {
        if (event.target.classList.contains('modal')) {
            event.target.style.display = 'none';
        }
    });
}

async function loadTodayData() {
    try {
        showLoading();
        
        const [meals, workouts, emotions] = await Promise.all([
            fetchData(`${API_BASE}/meals/today?userId=${CURRENT_USER_ID}`),
            fetchData(`${API_BASE}/workouts/today?userId=${CURRENT_USER_ID}`),
            fetchData(`${API_BASE}/emotions/today?userId=${CURRENT_USER_ID}`)
        ]);
        
        updateMealsDisplay(meals);
        updateWorkoutsDisplay(workouts);
        updateEmotionsDisplay(emotions);
        updateStats(meals, workouts);
        
        loadClaudeMessage();
        
    } catch (error) {
        console.error('데이터 로드 실패:', error);
        showNotification('데이터를 불러오는데 실패했습니다.', 'error');
    } finally {
        hideLoading();
    }
}

async function loadClaudeMessage() {
    try {
        const response = await fetchData(`${API_BASE}/claude/daily?userId=${CURRENT_USER_ID}`);
        document.getElementById('claude-message').innerHTML = response;
    } catch (error) {
        document.getElementById('claude-message').innerHTML = '오늘도 화이팅! 💪 (Claude 메시지를 불러올 수 없습니다)';
    }
}

async function refreshClaudeMessage() {
    const messageEl = document.getElementById('claude-message');
    messageEl.innerHTML = '<span class="loading">Claude가 새로운 조언을 준비 중...</span>';
    await loadClaudeMessage();
}

async function handleMealSubmit(event) {
    event.preventDefault();
    const formData = new FormData(event.target);
    const data = {
        userId: CURRENT_USER_ID,
        mealType: formData.get('mealType'),
        description: formData.get('description'),
        caloriesEstimate: formData.get('caloriesEstimate') ? parseInt(formData.get('caloriesEstimate')) : null
    };
    
    try {
        await fetchData(`${API_BASE}/meals`, 'POST', data);
        closeModal('meal-modal');
        event.target.reset();
        loadTodayData();
        showNotification('식사가 기록되었습니다!');
    } catch (error) {
        showNotification('식사 기록에 실패했습니다.', 'error');
    }
}

async function handleWorkoutSubmit(event) {
    event.preventDefault();
    const formData = new FormData(event.target);
    const data = {
        userId: CURRENT_USER_ID,
        type: formData.get('type'),
        duration: parseInt(formData.get('duration')),
        caloriesBurned: formData.get('caloriesBurned') ? parseInt(formData.get('caloriesBurned')) : null,
        notes: formData.get('notes') || null
    };
    
    try {
        await fetchData(`${API_BASE}/workouts`, 'POST', data);
        closeModal('workout-modal');
        event.target.reset();
        loadTodayData();
        showNotification('운동이 기록되었습니다!');
    } catch (error) {
        showNotification('운동 기록에 실패했습니다.', 'error');
    }
}

async function handleEmotionSubmit(event) {
    event.preventDefault();
    const formData = new FormData(event.target);
    const data = {
        userId: CURRENT_USER_ID,
        mood: formData.get('mood'),
        intensity: parseInt(formData.get('intensity')),
        note: formData.get('note') || null
    };
    
    try {
        await fetchData(`${API_BASE}/emotions`, 'POST', data);
        closeModal('emotion-modal');
        event.target.reset();
        loadTodayData();
        showNotification('감정이 기록되었습니다!');
    } catch (error) {
        showNotification('감정 기록에 실패했습니다.', 'error');
    }
}

function openModal(modalId) {
    document.getElementById(modalId).style.display = 'block';
}

function closeModal(modalId) {
    document.getElementById(modalId).style.display = 'none';
}

function showNotification(message, type = 'success') {
    const notification = document.createElement('div');
    notification.className = `notification ${type}`;
    notification.innerHTML = `<i class="fas ${type === 'success' ? 'fa-check' : 'fa-exclamation-triangle'}"></i> ${message}`;
    
    document.body.appendChild(notification);
    
    setTimeout(() => notification.classList.add('show'), 100);
    
    setTimeout(() => {
        notification.classList.remove('show');
        setTimeout(() => document.body.removeChild(notification), 300);
    }, 3000);
}

function showLoading() {
    document.body.style.cursor = 'wait';
}

function hideLoading() {
    document.body.style.cursor = 'default';
}

async function fetchData(url, method = 'GET', data = null) {
    const options = {
        method,
        headers: {
            'Content-Type': 'application/json'
        }
    };
    
    if (data) {
        options.body = JSON.stringify(data);
    }
    
    const response = await fetch(url, options);
    
    if (!response.ok) {
        throw new Error(`HTTP ${response.status}: ${response.statusText}`);
    }
    
    if (method === 'DELETE') {
        return null;
    }
    
    const contentType = response.headers.get('content-type');
    if (contentType && contentType.includes('application/json')) {
        return await response.json();
    } else {
        return await response.text();
    }
}

function updateMealsDisplay(meals) {
    const container = document.getElementById('meals-list');
    if (!meals || meals.length === 0) {
        container.innerHTML = '<div class="empty-state"><i class="fas fa-utensils"></i><p>아직 기록된 식사가 없습니다.</p></div>';
        return;
    }
}

function updateWorkoutsDisplay(workouts) {
    const container = document.getElementById('workouts-list');
    if (!workouts || workouts.length === 0) {
        container.innerHTML = '<div class="empty-state"><i class="fas fa-dumbbell"></i><p>아직 기록된 운동이 없습니다.</p></div>';
        return;
    }
}

function updateEmotionsDisplay(emotions) {
    const container = document.getElementById('emotions-list');
    if (!emotions || emotions.length === 0) {
        container.innerHTML = '<div class="empty-state"><i class="fas fa-smile"></i><p>아직 기록된 감정이 없습니다.</p></div>';
        return;
    }
}

function updateStats(meals, workouts) {
    const totalCalories = meals?.reduce((sum, meal) => sum + (meal.caloriesEstimate || 0), 0) || 0;
    const caloriesBurned = workouts?.reduce((sum, workout) => sum + (workout.caloriesBurned || 0), 0) || 0;
    const workoutTime = workouts?.reduce((sum, workout) => sum + workout.duration, 0) || 0;
    
    document.getElementById('total-calories').textContent = totalCalories;
    document.getElementById('meal-count').textContent = meals?.length || 0;
    document.getElementById('calories-burned').textContent = caloriesBurned;
    document.getElementById('workout-time').textContent = workoutTime;
    document.getElementById('net-calories').textContent = totalCalories - caloriesBurned;
    
    const calorieGoal = 2000;
    const progress = Math.min((totalCalories / calorieGoal) * 100, 100);
    document.getElementById('calorie-progress').style.width = progress + '%';
}

console.log('🍎 MyDiet 앱이 로드되었습니다!');