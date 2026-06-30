package com.example.blog.controller;

import com.example.blog.dto.LoginRequest;
import com.example.blog.dto.LoginResponse;
import com.example.blog.dto.RegisterRequest;
import com.example.blog.dto.UserProfileVo;
import com.example.blog.security.UserContext;
import com.example.blog.service.UserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ApiResponse<LoginResponse> register(@Valid @RequestBody RegisterRequest request) {
        try {
            LoginResponse response = userService.register(request);
            return ApiResponse.ok(response);
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        try {
            LoginResponse response = userService.login(request);
            return ApiResponse.ok(response);
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @GetMapping("/profile")
    public ApiResponse<UserProfileVo> profile() {
        Long userId = UserContext.getCurrentUserId();
        if (userId == null) {
            return ApiResponse.error("未登录");
        }
        try {
            UserProfileVo profile = userService.getProfile(userId);
            return ApiResponse.ok(profile);
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }
}
