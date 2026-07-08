package com.example.blog.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.blog.dto.CheckInVo;
import com.example.blog.dto.LoginRequest;
import com.example.blog.dto.LoginResponse;
import com.example.blog.dto.RegisterRequest;
import com.example.blog.dto.UserProfileVo;
import com.example.blog.entity.Post;

import java.util.List;

public interface UserService {
    LoginResponse register(RegisterRequest request);
    LoginResponse login(LoginRequest request);
    UserProfileVo getProfile(Long userId);
    UserProfileVo updateProfile(Long userId, UserProfileVo profileData);
    List<UserProfileVo> getAllUsers();
    IPage<Post> getMyPosts(Long userId, long page, long size, String status);
    void assignRole(Long userId, String roleName);
}
