package com.example.blog.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("messages")
public class Message {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long recipientId;

    private Long senderId;

    private String title;

    private String content;

    private String type;

    private Boolean isRead;

    private Long relatedPostId;

    private LocalDateTime createdAt;
}
