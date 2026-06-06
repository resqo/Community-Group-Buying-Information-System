package com.example.community_group_buy_backend.utils;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public final class JwtUtil {
    private static final String SECRET = "community-group-buy-secret";

    private JwtUtil() {
    }

    public static String createToken(Long userId, String username, String role) {
        long exp = Instant.now().plusSeconds(24 * 60 * 60).getEpochSecond();
        String header = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
        String payload = String.format(
                "{\"userId\":%d,\"username\":\"%s\",\"role\":\"%s\",\"exp\":%d}",
                userId, escape(username), escape(role), exp);
        String headerPart = encode(header.getBytes(StandardCharsets.UTF_8));
        String payloadPart = encode(payload.getBytes(StandardCharsets.UTF_8));
        String signaturePart = sign(headerPart + "." + payloadPart);
        return headerPart + "." + payloadPart + "." + signaturePart;
    }

    private static String sign(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return encode(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("生成 token 失败", ex);
        }
    }

    private static String encode(byte[] data) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(data);
    }

    private static String escape(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
