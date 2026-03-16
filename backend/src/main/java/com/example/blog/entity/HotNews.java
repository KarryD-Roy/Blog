package com.example.blog.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("hot_news")
public class HotNews {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private String url;
    private String imageUrl;
    private String source;
    private LocalDateTime publishDate;
}
