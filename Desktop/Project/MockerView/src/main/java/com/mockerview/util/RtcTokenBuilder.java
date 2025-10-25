package com.mockerview.util;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Base64;
import java.util.TreeMap;

public class RtcTokenBuilder {
    
    public enum Role {
        Role_Publisher(1),
        Role_Subscriber(2);
        
        public int value;
        
        Role(int value) {
            this.value = value;
        }
    }
    
    public String buildTokenWithUid(String appId, String appCertificate, String channelName, 
                                     int uid, Role role, int privilegeExpiredTs) throws Exception {
        return buildTokenWithUserAccount(appId, appCertificate, channelName, 
                                          String.valueOf(uid), role, privilegeExpiredTs);
    }
    
    public String buildTokenWithUserAccount(String appId, String appCertificate, String channelName,
                                             String account, Role role, int privilegeExpiredTs) throws Exception {
        
        int issueTs = (int)(System.currentTimeMillis() / 1000);
        int expire = privilegeExpiredTs - issueTs;
        int salt = (int)(Math.random() * 100000000);
        
        ByteArrayOutputStream signing = new ByteArrayOutputStream();
        
        packString(signing, appId);
        packUInt32(signing, issueTs);
        packUInt32(signing, expire);
        packUInt32(signing, salt);
        
        packUInt16(signing, (short)1);
        
        packUInt16(signing, (short)1);
        packString(signing, channelName);
        packString(signing, account);
        
        TreeMap<Short, Integer> privileges = new TreeMap<>();
        privileges.put((short)1, privilegeExpiredTs);
        if (role == Role.Role_Publisher) {
            privileges.put((short)2, privilegeExpiredTs);
            privileges.put((short)3, privilegeExpiredTs);
            privileges.put((short)4, privilegeExpiredTs);
        }
        
        packUInt16(signing, (short)privileges.size());
        for (Short key : privileges.keySet()) {
            packUInt16(signing, key);
            packUInt32(signing, privileges.get(key));
        }
        
        byte[] signingBytes = signing.toByteArray();
        byte[] signature = hmacSha256(appCertificate, signingBytes);
        
        ByteArrayOutputStream content = new ByteArrayOutputStream();
        packString(content, appId);
        packUInt32(content, issueTs);
        packUInt32(content, expire);
        packUInt32(content, salt);
        packUInt16(content, (short)signature.length);
        content.write(signature);
        
        return "007" + Base64.getEncoder().encodeToString(content.toByteArray());
    }
    
    private void packString(ByteArrayOutputStream out, String value) throws Exception {
        byte[] bytes = value.getBytes("UTF-8");
        packUInt16(out, (short)bytes.length);
        out.write(bytes);
    }
    
    private void packUInt32(ByteArrayOutputStream out, int value) {
        ByteBuffer buffer = ByteBuffer.allocate(4);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.putInt(value);
        out.write(buffer.array(), 0, 4);
    }
    
    private void packUInt16(ByteArrayOutputStream out, short value) {
        ByteBuffer buffer = ByteBuffer.allocate(2);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.putShort(value);
        out.write(buffer.array(), 0, 2);
    }
    
    private byte[] hmacSha256(String key, byte[] data) throws Exception {
        Mac hmac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKey = new SecretKeySpec(key.getBytes("UTF-8"), "HmacSHA256");
        hmac.init(secretKey);
        return hmac.doFinal(data);
    }
}
