package com.example.blog.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * user_roles 联合主键表 (user_id + role_id)。
 * 不使用 @TableId 注解 —— MyBatis-Plus 原生不支持多字段复合主键，
 * 此处将 userId/roleId 作为普通字段处理即可满足全部业务场景
 * （按 userId 查询、按 userId+roleId 插入/去重）。
 */
@Data
@TableName("user_roles")
public class UserRole {
    private Long userId;
    private Long roleId;
}
