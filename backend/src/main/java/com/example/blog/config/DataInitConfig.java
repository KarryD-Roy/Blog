package com.example.blog.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.blog.entity.Role;
import com.example.blog.entity.User;
import com.example.blog.entity.UserRole;
import com.example.blog.mapper.RoleMapper;
import com.example.blog.mapper.UserMapper;
import com.example.blog.mapper.UserRoleMapper;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class DataInitConfig implements CommandLineRunner {

    private final RoleMapper roleMapper;
    private final UserMapper userMapper;
    private final UserRoleMapper userRoleMapper;
    private final PasswordEncoder passwordEncoder;

    public DataInitConfig(RoleMapper roleMapper, UserMapper userMapper,
                          UserRoleMapper userRoleMapper, PasswordEncoder passwordEncoder) {
        this.roleMapper = roleMapper;
        this.userMapper = userMapper;
        this.userRoleMapper = userRoleMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        initRoles();
        initDefaultUsers();
    }

    private void initRoles() {
        if (roleMapper.selectCount(null) == 0) {
            Role adminRole = new Role();
            adminRole.setName("ADMIN");
            roleMapper.insert(adminRole);

            Role userRole = new Role();
            userRole.setName("USER");
            roleMapper.insert(userRole);

            System.out.println("Roles initialized: ADMIN, USER");
        }
    }

    private void initDefaultUsers() {
        if (userMapper.selectCount(null) == 0) {
            // Create admin user
            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setEmail("admin@blog.com");
            admin.setNickname("管理员");
            admin.setEnabled(true);
            admin.setCreatedAt(LocalDateTime.now());
            admin.setUpdatedAt(LocalDateTime.now());
            userMapper.insert(admin);
            assignRole(admin.getId(), "ADMIN");
            assignRole(admin.getId(), "USER");

            // Create test user
            User user = new User();
            user.setUsername("user");
            user.setPassword(passwordEncoder.encode("user123"));
            user.setEmail("user@blog.com");
            user.setNickname("普通用户");
            user.setEnabled(true);
            user.setCreatedAt(LocalDateTime.now());
            user.setUpdatedAt(LocalDateTime.now());
            userMapper.insert(user);
            assignRole(user.getId(), "USER");

            System.out.println("Default users created: admin/admin123, user/user123");
        }
    }

    private void assignRole(Long userId, String roleName) {
        LambdaQueryWrapper<Role> roleQuery = new LambdaQueryWrapper<>();
        roleQuery.eq(Role::getName, roleName);
        Role role = roleMapper.selectOne(roleQuery);
        if (role != null) {
            UserRole ur = new UserRole();
            ur.setUserId(userId);
            ur.setRoleId(role.getId());
            userRoleMapper.insert(ur);
        }
    }
}
