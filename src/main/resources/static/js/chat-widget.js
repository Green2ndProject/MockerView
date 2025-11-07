document.addEventListener('DOMContentLoaded', () => {
        const toggleBtn = document.getElementById('chat-toggle-btn');
        const closeBtn = document.getElementById('chat-close-btn');
        const widgetContainer = document.getElementById('chat-widget-container');

        const toggleChatWidget = () => {
            widgetContainer.classList.toggle('is-open');
        };

        if(toggleBtn){
            toggleBtn.addEventListener('click', toggleChatWidget);
        }
        if(closeBtn){
            closeBtn.addEventListener('click', toggleChatWidget);
        }    
});