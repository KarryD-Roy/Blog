package com.example.blog.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("post_skill_relation")
public class PostSkillRelation {

    @TableId(value = "post_id", type = IdType.INPUT)
    private Long postId;

    private Long skillId;
}
