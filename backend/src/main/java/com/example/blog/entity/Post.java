package com.example.blog.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("posts")
public class Post {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;

    private String summary;

    private String content;

    private Long userId;

    private Long categoryId;

    private String tags; // 以逗号分隔的标签

    private Integer viewCount;

    /**
     * 是否置顶，true 表示置顶
     */
    private Boolean pinned;

    /**
     * 审核状态：PUBLISHED, PENDING, REJECTED
     */
    private String status;

    @com.baomidou.mybatisplus.annotation.TableField(exist = false)
    private java.util.List<Long> skillIds;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
