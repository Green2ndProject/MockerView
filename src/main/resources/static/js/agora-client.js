class AgoraClient {
    constructor(appId) {
        this.appId = appId;
        this.client = AgoraRTC.createClient({ mode: 'rtc', codec: 'vp8' });
        this.localTracks = { audioTrack: null, videoTrack: null };
        this.remoteUsers = {};
        this.isJoined = false;
    }

    async join(channel, token, uid) {
        if (this.isJoined) return;

        await this.client.join(this.appId, channel, token, uid);
        this.isJoined = true;

        this.client.on('user-published', async (user, mediaType) => {
            await this.client.subscribe(user, mediaType);
            
            if (mediaType === 'video') {
                const remoteVideoTrack = user.videoTrack;
                const playerContainer = document.getElementById('remote-video-' + user.uid);
                if (!playerContainer) {
                    const newContainer = document.createElement('div');
                    newContainer.id = 'remote-video-' + user.uid;
                    newContainer.className = 'remote-video-container';
                    document.getElementById('remote-videos').appendChild(newContainer);
                }
                remoteVideoTrack.play('remote-video-' + user.uid);
            }
            
            if (mediaType === 'audio') {
                const remoteAudioTrack = user.audioTrack;
                remoteAudioTrack.play();
            }
        });

        this.client.on('user-unpublished', (user) => {
            const playerContainer = document.getElementById('remote-video-' + user.uid);
            if (playerContainer) {
                playerContainer.remove();
            }
        });

        this.client.on('user-left', (user) => {
            const playerContainer = document.getElementById('remote-video-' + user.uid);
            if (playerContainer) {
                playerContainer.remove();
            }
        });
    }

    async publishAudioVideo() {
        this.localTracks.audioTrack = await AgoraRTC.createMicrophoneAudioTrack();
        this.localTracks.videoTrack = await AgoraRTC.createCameraVideoTrack();

        const localPlayerContainer = document.getElementById('local-video');
        if (localPlayerContainer) {
            this.localTracks.videoTrack.play(localPlayerContainer);
        }

        await this.client.publish(Object.values(this.localTracks));
    }

    async publishAudioOnly() {
        this.localTracks.audioTrack = await AgoraRTC.createMicrophoneAudioTrack();
        await this.client.publish([this.localTracks.audioTrack]);
    }

    async leave() {
        if (!this.isJoined) return;

        for (let trackName in this.localTracks) {
            const track = this.localTracks[trackName];
            if (track) {
                track.stop();
                track.close();
            }
        }

        this.localTracks = { audioTrack: null, videoTrack: null };

        await this.client.leave();
        this.isJoined = false;

        const remoteVideos = document.getElementById('remote-videos');
        if (remoteVideos) {
            remoteVideos.innerHTML = '';
        }
    }

    toggleAudio() {
        if (this.localTracks.audioTrack) {
            const enabled = !this.localTracks.audioTrack.enabled;
            this.localTracks.audioTrack.setEnabled(enabled);
            return enabled;
        }
        return false;
    }

    toggleVideo() {
        if (this.localTracks.videoTrack) {
            const enabled = !this.localTracks.videoTrack.enabled;
            this.localTracks.videoTrack.setEnabled(enabled);
            return enabled;
        }
        return false;
    }
}
