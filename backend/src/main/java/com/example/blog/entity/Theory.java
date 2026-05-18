package com.example.blog.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("theories")
public class Theory {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long skillId;
    private String content;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
