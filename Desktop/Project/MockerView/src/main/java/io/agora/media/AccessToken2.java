package io.agora.media;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.TreeMap;
import java.util.zip.CRC32;

public class AccessToken2 {
    public static final short kServiceRtc = 1;
    public static final short kServiceRtm = 2;
    public static final short kServiceFpa = 4;
    public static final short kServiceChat = 5;
    public static final short kServiceEducation = 7;

    private String _appId = "";
    private String _appCert = "";
    private int _issueTs = 0;
    private int _expire = 900;
    private int _salt = 0;
    private TreeMap<Short, Service> _services = new TreeMap<>();

    public AccessToken2(String appId, String appCert, int expire) {
        _appId = appId;
        _appCert = appCert;
        _expire = expire;
        _issueTs = (int) (System.currentTimeMillis() / 1000);
        _salt = (int) (Math.random() * 100000000);
    }

    public void addService(Service service) {
        _services.put(service.__service_type, service);
    }

    public String build() throws Exception {
        if (!Utils.isUUIDValid(_appId)) {
            System.err.println("❌ AccessToken2.build() - Invalid App ID: " + _appId);
            System.err.println("   App ID Length: " + (_appId != null ? _appId.length() : "NULL"));
            System.err.println("   Expected: 32 chars, hex only");
            throw new Exception("Invalid App ID: " + _appId);
        }
    
        byte[] signing = buildSigning();
        byte[] signature = Utils.hmacSign(_appCert, signing);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Utils.packString(baos, _appId);
        Utils.packUInt32(baos, _issueTs);
        Utils.packUInt32(baos, _expire);
        Utils.packUInt32(baos, _salt);
        Utils.packUInt16(baos, (short) signature.length);
        baos.write(signature);

        return "006" + Base64.getEncoder().encodeToString(baos.toByteArray());
    }

    private byte[] buildSigning() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Utils.packString(baos, _appId);
        Utils.packUInt32(baos, _issueTs);
        Utils.packUInt32(baos, _expire);
        Utils.packUInt32(baos, _salt);
        Utils.packUInt16(baos, (short) _services.size());

        for (Service service : _services.values()) {
            baos.write(service.pack());
        }

        return baos.toByteArray();
    }

    public static abstract class Service {
        protected short __service_type;
        protected TreeMap<Short, Integer> __privileges;

        public Service(short serviceType) {
            __service_type = serviceType;
            __privileges = new TreeMap<>();
        }

        public void addPrivilege(short privilege, int expire) {
            __privileges.put(privilege, expire);
        }

        abstract byte[] pack() throws Exception;
    }

    public static class ServiceRtc extends Service {
        public static final short kPrivilegeJoinChannel = 1;
        public static final short kPrivilegePublishAudioStream = 2;
        public static final short kPrivilegePublishVideoStream = 3;
        public static final short kPrivilegePublishDataStream = 4;

        private String __channel_name;
        private String __uid;

        public ServiceRtc(String channelName, String uid) {
            super(kServiceRtc);
            __channel_name = channelName;
            __uid = uid;
        }

        @Override
        public byte[] pack() throws Exception {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Utils.packUInt16(baos, __service_type);
            Utils.packString(baos, __channel_name);
            Utils.packString(baos, __uid);
            Utils.packMapUInt32(baos, __privileges);
            return baos.toByteArray();
        }
    }
}

class Utils {
    static boolean isUUIDValid(String uuid) {
        if (uuid.length() != 32) {
            return false;
        }
        return uuid.matches("[0-9a-fA-F]+");
    }

    static byte[] hmacSign(String key, byte[] data) throws Exception {
        Mac hmac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKey = new SecretKeySpec(key.getBytes("UTF-8"), "HmacSHA256");
        hmac.init(secretKey);
        return hmac.doFinal(data);
    }

    static void packString(ByteArrayOutputStream out, String value) throws Exception {
        byte[] bytes = value.getBytes("UTF-8");
        packUInt16(out, (short) bytes.length);
        out.write(bytes);
    }

    static void packUInt32(ByteArrayOutputStream out, int value) {
        ByteBuffer buffer = ByteBuffer.allocate(4);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.putInt(value);
        out.write(buffer.array(), 0, 4);
    }

    static void packUInt16(ByteArrayOutputStream out, short value) {
        ByteBuffer buffer = ByteBuffer.allocate(2);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.putShort(value);
        out.write(buffer.array(), 0, 2);
    }

    static void packMapUInt32(ByteArrayOutputStream out, TreeMap<Short, Integer> map) {
        packUInt16(out, (short) map.size());
        for (Short key : map.keySet()) {
            packUInt16(out, key);
            packUInt32(out, map.get(key));
        }
    }
}