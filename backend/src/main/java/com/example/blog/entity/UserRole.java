package com.example.blog.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("user_roles")
public class UserRole {
    @TableId(type = IdType.ASSIGN_ID)
    private UserRoleId id;

    private Long userId;
    private Long roleId;
}
