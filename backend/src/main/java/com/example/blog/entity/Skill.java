package com.example.blog.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("skills")
public class Skill {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String category;

    private String title;

    private String description;

    /**
     * 是否置顶，true 表示置顶
     */
    private Boolean pinned;
}

