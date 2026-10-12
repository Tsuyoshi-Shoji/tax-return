package org.example.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.features.auth.AuthSessionAttributes;
import org.springframework.web.servlet.HandlerInterceptor;

public class AuthRequiredInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        Object user = request.getSession(false) == null
                ? null
                : request.getSession(false).getAttribute(AuthSessionAttributes.CURRENT_USER);
        if (user != null) {
            return true;
        }
        response.sendRedirect(request.getContextPath() + "/login");
        return false;
    }
}
