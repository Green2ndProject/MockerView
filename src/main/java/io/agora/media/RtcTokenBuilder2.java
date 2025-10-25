package io.agora.media;

public class RtcTokenBuilder2 {
    public enum Role {
        ROLE_PUBLISHER(1),
        ROLE_SUBSCRIBER(2);

        public int initValue;

        Role(int initValue) {
            this.initValue = initValue;
        }
    }

    public String buildTokenWithUid(String appId, String appCertificate, String channelName,
                                     int uid, Role role, int tokenExpire, int privilegeExpire) throws Exception {
        return buildTokenWithUserAccount(appId, appCertificate, channelName,
                String.valueOf(uid), role, tokenExpire, privilegeExpire);
    }

    public String buildTokenWithUserAccount(String appId, String appCertificate, String channelName,
                                             String account, Role role, int tokenExpire, int privilegeExpire) throws Exception {
        AccessToken2 accessToken = new AccessToken2(appId, appCertificate, tokenExpire);
        AccessToken2.ServiceRtc serviceRtc = new AccessToken2.ServiceRtc(channelName, account);

        serviceRtc.addPrivilege(AccessToken2.ServiceRtc.kPrivilegeJoinChannel, privilegeExpire);
        
        if (role == Role.ROLE_PUBLISHER) {
            serviceRtc.addPrivilege(AccessToken2.ServiceRtc.kPrivilegePublishAudioStream, privilegeExpire);
            serviceRtc.addPrivilege(AccessToken2.ServiceRtc.kPrivilegePublishVideoStream, privilegeExpire);
            serviceRtc.addPrivilege(AccessToken2.ServiceRtc.kPrivilegePublishDataStream, privilegeExpire);
        }

        accessToken.addService(serviceRtc);
        return accessToken.build();
    }
}
