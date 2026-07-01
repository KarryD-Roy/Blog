package com.example.blog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.blog.dto.LoginRequest;
import com.example.blog.dto.LoginResponse;
import com.example.blog.dto.RegisterRequest;
import com.example.blog.dto.UserProfileVo;
import com.example.blog.entity.Role;
import com.example.blog.entity.User;
import com.example.blog.entity.UserRole;
import com.example.blog.mapper.RoleMapper;
import com.example.blog.mapper.UserMapper;
import com.example.blog.mapper.UserRoleMapper;
import com.example.blog.security.JwtUtil;
import com.example.blog.service.UserService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final UserRoleMapper userRoleMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public UserServiceImpl(UserMapper userMapper, RoleMapper roleMapper,
                           UserRoleMapper userRoleMapper, PasswordEncoder passwordEncoder,
                           JwtUtil jwtUtil) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.userRoleMapper = userRoleMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @Override
    @Transactional
    public LoginResponse register(RegisterRequest request) {
        LambdaQueryWrapper<User> query = new LambdaQueryWrapper<>();
        query.eq(User::getUsername, request.getUsername());
        if (userMapper.selectCount(query) > 0) {
            throw new RuntimeException("用户名已存在");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmail(request.getEmail());
        user.setNickname(request.getNickname() != null ? request.getNickname() : request.getUsername());
        user.setEnabled(true);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.insert(user);

        // Assign ROLE_USER by default
        LambdaQueryWrapper<Role> roleQuery = new LambdaQueryWrapper<>();
        roleQuery.eq(Role::getName, "USER");
        Role defaultRole = roleMapper.selectOne(roleQuery);
        if (defaultRole != null) {
            UserRole userRole = new UserRole();
            userRole.setUserId(user.getId());
            userRole.setRoleId(defaultRole.getId());
            userRoleMapper.insert(userRole);
        }

        List<String> roles = getUserRoles(user.getId());
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), roles);

        return buildLoginResponse(user, roles, token);
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        LambdaQueryWrapper<User> query = new LambdaQueryWrapper<>();
        query.eq(User::getUsername, request.getUsername());
        User user = userMapper.selectOne(query);

        if (user == null || !user.getEnabled()) {
            throw new RuntimeException("用户名或密码错误");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("用户名或密码错误");
        }

        List<String> roles = getUserRoles(user.getId());
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), roles);

        return buildLoginResponse(user, roles, token);
    }

    @Override
    public UserProfileVo getProfile(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        UserProfileVo vo = new UserProfileVo();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setEmail(user.getEmail());
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());
        vo.setBio(user.getBio());
        vo.setRoles(getUserRoles(userId));
        vo.setCreatedAt(user.getCreatedAt());
        return vo;
    }

    @Override
    public List<UserProfileVo> getAllUsers() {
        List<User> users = userMapper.selectList(null);
        List<UserProfileVo> result = new ArrayList<>();
        for (User user : users) {
            UserProfileVo vo = new UserProfileVo();
            vo.setId(user.getId());
            vo.setUsername(user.getUsername());
            vo.setEmail(user.getEmail());
            vo.setNickname(user.getNickname());
            vo.setAvatar(user.getAvatar());
            vo.setBio(user.getBio());
            vo.setRoles(getUserRoles(user.getId()));
            vo.setCreatedAt(user.getCreatedAt());
            result.add(vo);
        }
        return result;
    }

    @Override
    @Transactional
    public void assignRole(Long userId, String roleName) {
        LambdaQueryWrapper<Role> roleQuery = new LambdaQueryWrapper<>();
        roleQuery.eq(Role::getName, roleName);
        Role role = roleMapper.selectOne(roleQuery);
        if (role == null) {
            throw new RuntimeException("角色不存在: " + roleName);
        }

        LambdaQueryWrapper<UserRole> urQuery = new LambdaQueryWrapper<>();
        urQuery.eq(UserRole::getUserId, userId)
               .eq(UserRole::getRoleId, role.getId());
        if (userRoleMapper.selectCount(urQuery) > 0) {
            return; // already assigned
        }

        UserRole userRole = new UserRole();
        userRole.setUserId(userId);
        userRole.setRoleId(role.getId());
        userRoleMapper.insert(userRole);
    }

    private List<String> getUserRoles(Long userId) {
        LambdaQueryWrapper<UserRole> query = new LambdaQueryWrapper<>();
        query.eq(UserRole::getUserId, userId);
        List<UserRole> userRoles = userRoleMapper.selectList(query);
        if (userRoles == null || userRoles.isEmpty()) {
            return List.of("USER");
        }
        List<Long> roleIds = userRoles.stream().map(UserRole::getRoleId).collect(Collectors.toList());
        if (roleIds.isEmpty()) {
            return List.of("USER");
        }
        List<Role> roles = roleMapper.selectBatchIds(roleIds);
        if (roles == null || roles.isEmpty()) {
            return List.of("USER");
        }
        return roles.stream().map(Role::getName).collect(Collectors.toList());
    }

    private LoginResponse buildLoginResponse(User user, List<String> roles, String token) {
        LoginResponse response = new LoginResponse();
        response.setToken(token);
        response.setUserId(user.getId());
        response.setUsername(user.getUsername());
        response.setNickname(user.getNickname());
        response.setRoles(roles);
        return response;
    }
}
