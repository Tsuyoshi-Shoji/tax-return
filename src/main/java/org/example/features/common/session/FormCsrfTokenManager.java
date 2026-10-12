package org.example.features.common.session;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpSession;

@Component
public class FormCsrfTokenManager {
    private static final String SESSION_PREFIX = FormCsrfTokenManager.class.getName() + ".";

    private final SecureRandom secureRandom = new SecureRandom();

    public String issue(HttpSession session, String scope) {
        String key = SESSION_PREFIX + scope;
        synchronized (session) {
            Object existing = session.getAttribute(key);
            if (existing instanceof String token) {
                return token;
            }
            byte[] bytes = new byte[32];
            secureRandom.nextBytes(bytes);
            String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
            session.setAttribute(key, token);
            return token;
        }
    }

    public boolean isValid(HttpSession session, String scope, String provided) {
        Object expected = session.getAttribute(SESSION_PREFIX + scope);
        if (!(expected instanceof String expectedToken) || provided == null) {
            return false;
        }
        return MessageDigest.isEqual(
                expectedToken.getBytes(StandardCharsets.UTF_8),
                provided.getBytes(StandardCharsets.UTF_8));
    }
}