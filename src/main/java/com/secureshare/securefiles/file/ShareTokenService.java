package com.secureshare.securefiles.file;

import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.regex.Pattern;

@Service
public class ShareTokenService {
    private final SecureRandom secureRandom = new SecureRandom();
    private static final Pattern TOKEN_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]{32,64}$");

    public String generateSecureToken() {
        byte[] tokenBytes = new byte[32]; // 256-bit token
        secureRandom.nextBytes(tokenBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
    }

    public String generateShortToken() {
        byte[] tokenBytes = new byte[16]; // 128-bit token
        secureRandom.nextBytes(tokenBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
    }

    public boolean isValidTokenFormat(String token) {
        return token != null && TOKEN_PATTERN.matcher(token).matches();
    }
}