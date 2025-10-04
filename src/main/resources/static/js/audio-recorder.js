class AudioRecorder {
    constructor() {
        this.mediaRecorder = null;
        this.audioChunks = [];
        this.isRecording = false;
        this.stream = null;
    }

    async start() {
        try {
            this.stream = await navigator.mediaDevices.getUserMedia({ audio: true });
            this.mediaRecorder = new MediaRecorder(this.stream);
            this.audioChunks = [];

            this.mediaRecorder.ondataavailable = (event) => {
                this.audioChunks.push(event.data);
            };

            this.mediaRecorder.start();
            this.isRecording = true;
            console.log('녹음 시작');
        } catch (error) {
            console.error('녹음 시작 실패:', error);
            throw error;
        }
    }

    stop() {
        return new Promise((resolve) => {
            if (!this.mediaRecorder || !this.isRecording) {
                resolve(null);
                return;
            }

            this.mediaRecorder.onstop = () => {
                const audioBlob = new Blob(this.audioChunks, { type: 'audio/webm' });
                this.isRecording = false;
                
                if (this.stream) {
                    this.stream.getTracks().forEach(track => track.stop());
                }
                
                console.log('녹음 완료, 크기:', audioBlob.size);
                resolve(audioBlob);
            };

            this.mediaRecorder.stop();
        });
    }

    async uploadAudio(audioBlob, questionId, sessionId) {
        const formData = new FormData();
        formData.append('audio', audioBlob, 'answer.webm');
        formData.append('questionId', questionId);
        formData.append('sessionId', sessionId);

        const token = this.getToken();
        
        try {
            const response = await fetch('/api/selfinterview/transcribe', {
                method: 'POST',
                headers: {
                    'Authorization': 'Bearer ' + token
                },
                body: formData
            });

            if (!response.ok) {
                throw new Error('음성 업로드 실패');
            }

            return await response.json();
        } catch (error) {
            console.error('음성 업로드 실패:', error);
            throw error;
        }
    }

    getToken() {
        const cookies = document.cookie.split(';');
        for (let cookie of cookies) {
            const [name, value] = cookie.trim().split('=');
            if (name === 'Authorization') {
                return value;
            }
        }
        return '';
    }
}
