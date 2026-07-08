package com.example.blog.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.blog.dto.CheckInVo;
import com.example.blog.dto.UserProfileVo;
import com.example.blog.entity.Post;
import com.example.blog.security.UserContext;
import com.example.blog.service.CheckInService;
import com.example.blog.service.UserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final CheckInService checkInService;

    public UserController(UserService userService, CheckInService checkInService) {
        this.userService = userService;
        this.checkInService = checkInService;
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

    @PutMapping("/profile")
    public ApiResponse<UserProfileVo> updateProfile(@RequestBody UserProfileVo profileData) {
        Long userId = UserContext.getCurrentUserId();
        if (userId == null) {
            return ApiResponse.error("未登录");
        }
        try {
            UserProfileVo updated = userService.updateProfile(userId, profileData);
            return ApiResponse.ok(updated);
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    /**
     * 获取当前用户的文章列表（个人文章管理）
     */
    @GetMapping("/posts")
    public ApiResponse<IPage<Post>> myPosts(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String status) {
        Long userId = UserContext.getCurrentUserId();
        if (userId == null) {
            return ApiResponse.error("请先登录");
        }
        return ApiResponse.ok(userService.getMyPosts(userId, page, size, status));
    }

    @PostMapping("/checkin")
    public ApiResponse<CheckInVo> checkIn() {
        Long userId = UserContext.getCurrentUserId();
        if (userId == null) {
            return ApiResponse.error("请先登录");
        }
        return ApiResponse.ok(checkInService.checkIn(userId));
    }

    @GetMapping("/checkin/status")
    public ApiResponse<CheckInVo> checkinStatus() {
        Long userId = UserContext.getCurrentUserId();
        if (userId == null) {
            return ApiResponse.error("请先登录");
        }
        return ApiResponse.ok(checkInService.getCheckInStatus(userId));
    }

    /**
     * ADMIN: 获取所有用户列表（角色管理用）
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<UserProfileVo>> listUsers() {
        return ApiResponse.ok(userService.getAllUsers());
    }

    /**
     * ADMIN: 角色授权接口 - 将普通用户升级为ADMIN
     */
    @PutMapping("/{userId}/role/{roleName}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> assignRole(
            @PathVariable Long userId,
            @PathVariable String roleName) {
        try {
            userService.assignRole(userId, roleName.toUpperCase());
            return ApiResponse.ok(null);
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }
}
