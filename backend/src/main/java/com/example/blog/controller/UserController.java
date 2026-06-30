package com.example.blog.controller;

import com.example.blog.dto.UserProfileVo;
import com.example.blog.service.UserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/profile")
    public ApiResponse<UserProfileVo> profile() {
        Long userId = com.example.blog.security.UserContext.getCurrentUserId();
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

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<UserProfileVo>> listUsers() {
        return ApiResponse.ok(userService.getAllUsers());
    }
}
