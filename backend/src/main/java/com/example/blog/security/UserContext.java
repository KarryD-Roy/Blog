package com.example.blog.security;

/**
 * ThreadLocal 上下文，用于在请求生命周期内透传当前用户ID。
 * 务必在请求结束时调用 clear() 释放资源，防止内存泄漏。
 */
public class UserContext {

    private static final ThreadLocal<Long> CURRENT_USER_ID = new ThreadLocal<>();

    public static void setCurrentUserId(Long userId) {
        CURRENT_USER_ID.set(userId);
    }

    public static Long getCurrentUserId() {
        return CURRENT_USER_ID.get();
    }

    public static void clear() {
        CURRENT_USER_ID.remove();
    }
}
