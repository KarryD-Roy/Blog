package com.example.blog.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MessageVo {
    private Long id;
    private String title;
    private String content;
    private String type;
    private Boolean isRead;
    private Long senderId;
    private String senderName;
    private Long relatedPostId;
    private LocalDateTime createdAt;
}
