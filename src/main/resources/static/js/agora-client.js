class AgoraClient {
    constructor(appId) {
        console.log('🎬 AgoraClient 생성:', appId);
        this.appId = appId;
        this.client = AgoraRTC.createClient({ mode: 'rtc', codec: 'vp8' });
        this.localAudioTrack = null;
        this.localVideoTrack = null;
        this.audioEnabled = true;
        this.videoEnabled = true;
        this.isJoined = false;
        this.userRoles = new Map();
        this.setupEventHandlers();
    }

    setupEventHandlers() {
        this.client.on('user-published', async (user, mediaType) => {
            console.log('👤 사용자 발행:', user.uid, mediaType);
            await this.client.subscribe(user, mediaType);
            console.log('✅ 구독 완료:', user.uid);
            
            if (mediaType === 'video') {
                const remoteVideoDiv = document.createElement('div');
                remoteVideoDiv.id = `remote-video-${user.uid}`;
                remoteVideoDiv.className = 'remote-video-container';
                remoteVideoDiv.style.position = 'relative';
                
                const roleLabel = document.createElement('div');
                roleLabel.className = 'remote-video-label';
                roleLabel.textContent = this.getUserRole(user.uid);
                remoteVideoDiv.appendChild(roleLabel);
                
                document.getElementById('remote-videos')?.appendChild(remoteVideoDiv);
                user.videoTrack.play(remoteVideoDiv.id);
            }
            
            if (mediaType === 'audio') {
                user.audioTrack.play();
            }
        });

        this.client.on('user-unpublished', (user, mediaType) => {
            console.log('👋 사용자 발행 취소:', user.uid, mediaType);
            if (mediaType === 'video') {
                const remoteVideoDiv = document.getElementById(`remote-video-${user.uid}`);
                if (remoteVideoDiv) {
                    remoteVideoDiv.remove();
                }
            }
        });

        this.client.on('connection-state-change', (curState, prevState, reason) => {
            console.log('🔌 연결 상태 변경:', {
                from: prevState,
                to: curState,
                reason: reason
            });
        });
    }

    getUserRole(uid) {
        if (this.userRoles.has(uid)) {
            return this.userRoles.get(uid);
        }
        return '참가자';
    }

    setUserRole(uid, role) {
        this.userRoles.set(uid, role);
    }

    async join(channel, token, uid) {
        console.log('🚀 채널 참가 시도:', {
            appId: this.appId,
            channel: channel,
            token: token ? token.substring(0, 20) + '...' : 'null',
            uid: uid
        });
        
        try {
            const assignedUid = await this.client.join(this.appId, channel, token, uid);
            this.isJoined = true;
            console.log('✅ 채널 참가 성공! UID:', assignedUid);
            return assignedUid;
        } catch (error) {
            console.error('❌ 채널 참가 실패:', error);
            console.error('  - 오류 코드:', error.code);
            console.error('  - 오류 메시지:', error.message);
            throw error;
        }
    }

    async publishAudioVideo() {
        console.log('🎤📹 오디오/비디오 발행 시작...');
        try {
            this.localAudioTrack = await AgoraRTC.createMicrophoneAudioTrack();
            console.log('✅ 마이크 트랙 생성');
            
            this.localVideoTrack = await AgoraRTC.createCameraVideoTrack();
            console.log('✅ 카메라 트랙 생성');
            
            this.localVideoTrack.play('local-video');
            console.log('✅ 로컬 비디오 재생');
            
            await this.client.publish([this.localAudioTrack, this.localVideoTrack]);
            console.log('✅ 오디오/비디오 발행 완료');
        } catch (error) {
            console.error('❌ 미디어 발행 실패:', error);
            throw error;
        }
    }

    async publishAudioOnly() {
        console.log('🎤 오디오 발행 시작...');
        try {
            this.localAudioTrack = await AgoraRTC.createMicrophoneAudioTrack();
            console.log('✅ 마이크 트랙 생성');
            
            await this.client.publish([this.localAudioTrack]);
            console.log('✅ 오디오 발행 완료');
        } catch (error) {
            console.error('❌ 오디오 발행 실패:', error);
            throw error;
        }
    }

    toggleAudio() {
        if (this.localAudioTrack) {
            this.audioEnabled = !this.audioEnabled;
            this.localAudioTrack.setEnabled(this.audioEnabled);
            console.log('🎤 오디오:', this.audioEnabled ? 'ON' : 'OFF');
        }
        return this.audioEnabled;
    }

    toggleVideo() {
        if (this.localVideoTrack) {
            this.videoEnabled = !this.videoEnabled;
            this.localVideoTrack.setEnabled(this.videoEnabled);
            console.log('📹 비디오:', this.videoEnabled ? 'ON' : 'OFF');
        }
        return this.videoEnabled;
    }

    async leave() {
        console.log('👋 채널 나가기...');
        
        try {
            if (this.localAudioTrack) {
                this.localAudioTrack.stop();
                this.localAudioTrack.close();
                this.localAudioTrack = null;
                console.log('✅ 오디오 트랙 닫음');
            }
            
            if (this.localVideoTrack) {
                this.localVideoTrack.stop();
                this.localVideoTrack.close();
                this.localVideoTrack = null;
                console.log('✅ 비디오 트랙 닫음');
            }
            
            if (this.isJoined) {
                await this.client.leave();
                this.isJoined = false;
                console.log('✅ 채널 나감');
            }
        } catch (error) {
            console.error('⚠️ 종료 중 오류 (무시):', error);
        }
    }
}