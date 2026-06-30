package com.example.blog.service;

import com.example.blog.dto.LoginRequest;
import com.example.blog.dto.LoginResponse;
import com.example.blog.dto.RegisterRequest;
import com.example.blog.dto.UserProfileVo;

import java.util.List;

public interface UserService {
    LoginResponse register(RegisterRequest request);
    LoginResponse login(LoginRequest request);
    UserProfileVo getProfile(Long userId);
    List<UserProfileVo> getAllUsers();
    void assignRole(Long userId, String roleName);
}
