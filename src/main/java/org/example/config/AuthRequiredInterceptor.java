package org.example.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.example.features.auth.AuthSessionAttributes;
import org.example.features.auth.AuthUserContext;
import org.springframework.web.servlet.HandlerInterceptor;

public class AuthRequiredInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute(AuthSessionAttributes.USER_ID) instanceof Long userId) {
            AuthUserContext.setCurrentUserId(userId);
            return true;
        }

        response.sendRedirect(request.getContextPath() + "/login");
        return false;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        AuthUserContext.clear();
    }
}

