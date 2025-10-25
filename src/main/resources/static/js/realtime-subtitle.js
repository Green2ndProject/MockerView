class RealtimeSubtitleManager {
    constructor(sessionId, userId, userName) {
        this.sessionId = sessionId;
        this.userId = userId;
        this.userName = userName;
        this.isRecording = false;
        this.mediaRecorder = null;
        this.audioChunks = [];
        this.audioContext = null;
        this.analyser = null;
        this.volumeThreshold = 1.5;
    }

    async startSubtitles() {
        try {
            const stream = await navigator.mediaDevices.getUserMedia({ 
                audio: {
                    echoCancellation: true,
                    noiseSuppression: true,
                    autoGainControl: true,
                    sampleRate: 16000
                } 
            });

            this.audioContext = new AudioContext({ sampleRate: 16000 });
            const source = this.audioContext.createMediaStreamSource(stream);
            this.analyser = this.audioContext.createAnalyser();
            this.analyser.fftSize = 2048;
            this.analyser.smoothingTimeConstant = 0.8;
            source.connect(this.analyser);

            const options = {
                mimeType: 'audio/webm;codecs=opus',
                audioBitsPerSecond: 48000
            };

            this.mediaRecorder = new MediaRecorder(stream, options);
            this.audioChunks = [];

            this.mediaRecorder.ondataavailable = (e) => {
                if (e.data.size > 0) {
                    this.audioChunks.push(e.data);
                }
            };

            this.mediaRecorder.onstop = async () => {
                const audioBlob = new Blob(this.audioChunks, { type: 'audio/webm;codecs=opus' });
                const hasVoice = this.hasVoiceActivity();
                
                console.log('📊 오디오 체크:', {
                    size: audioBlob.size,
                    hasVoice: hasVoice,
                    threshold: this.volumeThreshold
                });
                
                if (audioBlob.size > 5000 && hasVoice) {
                    console.log('✅ 음성 감지! 전송 중...');
                    await this.sendAudioForTranscription(audioBlob);
                } else {
                    console.log('⏭️ 스킵 (음성 없음 또는 파일 너무 작음)');
                }
                
                this.audioChunks = [];
                
                if (this.isRecording) {
                    this.mediaRecorder.start();
                    setTimeout(() => {
                        if (this.isRecording) {
                            this.mediaRecorder.stop();
                        }
                    }, 5000);
                }
            };

            this.isRecording = true;
            this.mediaRecorder.start();
            
            setTimeout(() => {
                if (this.isRecording) {
                    this.mediaRecorder.stop();
                }
            }, 5000);

            console.log('✅ 자막 녹음 시작 (5초 간격, 임계값:', this.volumeThreshold + ')');
        } catch (error) {
            console.error('❌ 자막 녹음 실패:', error);
            alert('마이크 권한을 허용해주세요');
        }
    }

    hasVoiceActivity() {
        if (!this.analyser) return true;

        const bufferLength = this.analyser.frequencyBinCount;
        const dataArray = new Uint8Array(bufferLength);
        this.analyser.getByteFrequencyData(dataArray);

        let sum = 0;
        for (let i = 0; i < bufferLength; i++) {
            sum += dataArray[i];
        }
        const average = sum / bufferLength;

        console.log('🎤 평균 음량:', average.toFixed(2), '/ 임계값:', this.volumeThreshold);
        
        return average > this.volumeThreshold;
    }

    setVolumeThreshold(value) {
        this.volumeThreshold = value;
        console.log('🎚️ 임계값 변경:', value);
    }

    stopSubtitles() {
        this.isRecording = false;
        if (this.mediaRecorder && this.mediaRecorder.state === 'recording') {
            this.mediaRecorder.stop();
            this.mediaRecorder.stream.getTracks().forEach(track => track.stop());
        }
        if (this.audioContext) {
            this.audioContext.close();
        }
        console.log('🛑 자막 녹음 중지');
    }

    async sendAudioForTranscription(audioBlob) {
        const formData = new FormData();
        formData.append('audio', audioBlob, 'subtitle.webm');

        try {
            const token = this.getToken();
            const response = await fetch(`/api/subtitle/${this.sessionId}/transcribe`, {
                method: 'POST',
                headers: { 'Authorization': token },
                body: formData
            });

            const result = await response.json();
            
            if (response.ok) {
                console.log('✅ 자막 API 성공:', result);
            } else {
                console.error('❌ 자막 API 실패:', result);
            }
        } catch (error) {
            console.error('❌ 자막 전송 오류:', error);
        }
    }

    getToken() {
        const cookies = document.cookie.split(';');
        for (let cookie of cookies) {
            const [name, value] = cookie.trim().split('=');
            if (name === 'Authorization') return value;
        }
        return '';
    }
}

function displaySubtitle(subtitle) {
    const container = document.getElementById('subtitle-content');
    if (!container) return;

    const userName = subtitle.userName || '알 수 없음';
    const text = subtitle.text || '';

    if (!text.trim()) return;

    const subtitleEl = document.createElement('div');
    subtitleEl.className = 'subtitle-item';
    subtitleEl.innerHTML = `
        <span class="subtitle-user">${userName}:</span>
        <span class="subtitle-text">${text}</span>
    `;

    container.appendChild(subtitleEl);
    container.scrollTop = container.scrollHeight;

    setTimeout(() => {
        if (container.children.length > 20) {
            container.removeChild(container.firstChild);
        }
    }, 100);
}

window.displaySubtitle = displaySubtitle;
