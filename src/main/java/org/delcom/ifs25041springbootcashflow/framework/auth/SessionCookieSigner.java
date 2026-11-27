package org.delcom.ifs25041springbootcashflow.framework.auth;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Optional;

/**
 * Menandatangani nilai cookie sesi dengan HMAC-SHA256 sehingga ID pengguna di cookie
 * tidak bisa dipalsukan oleh klien. Format nilai: {@code <userId>.<tanda-tangan-base64url>}.
 */
final class SessionCookieSigner {
    private static final String ALGORITHM = "HmacSHA256";

    private final byte[] key;

    /**
     * @param secret kunci rahasia; jika null/kosong dibuat acak (cookie lama tidak valid lagi setelah restart)
     */
    SessionCookieSigner(String secret) {
        if (secret == null || secret.isBlank()) {
            byte[] random = new byte[32];
            new SecureRandom().nextBytes(random);
            this.key = random;
        } else {
            this.key = secret.getBytes(StandardCharsets.UTF_8);
        }
    }

    String sign(int userId) {
        String payload = String.valueOf(userId);
        return payload + "." + Base64.getUrlEncoder().withoutPadding().encodeToString(mac(payload));
    }

    /** Mengembalikan ID pengguna jika nilai cookie utuh dan tanda tangannya benar. */
    Optional<Integer> verify(String cookieValue) {
        if (cookieValue == null) {
            return Optional.empty();
        }
        int dot = cookieValue.indexOf('.');
        if (dot <= 0) {
            return Optional.empty();
        }
        String payload = cookieValue.substring(0, dot);
        byte[] provided;
        int userId;
        try {
            userId = Integer.parseInt(payload);
            provided = Base64.getUrlDecoder().decode(cookieValue.substring(dot + 1));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
        // Perbandingan waktu-konstan agar tanda tangan tidak bisa ditebak lewat timing.
        if (!MessageDigest.isEqual(mac(payload), provided)) {
            return Optional.empty();
        }
        return Optional.of(userId);
    }

    private byte[] mac(String payload) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(key, ALGORITHM));
            return mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("HMAC-SHA256 tidak tersedia", ex);
        }
    }
}
