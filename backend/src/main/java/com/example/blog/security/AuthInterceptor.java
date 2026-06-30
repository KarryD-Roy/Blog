package com.example.blog.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) {
        Long userId = UserContext.getCurrentUserId();
        if (userId != null) {
            request.setAttribute("currentUserId", userId);
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        // CRITICAL: 防止内存泄漏，必须在请求完成后清除 ThreadLocal
        UserContext.clear();
    }
}
