package com.example.blog.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.blog.entity.HotNews;
import com.example.blog.service.HotNewsService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@RestController
@RequestMapping("/api/hot-news")
@RequiredArgsConstructor
public class HotNewsController {

    private final HotNewsService hotNewsService;

    /**
     * 获取热点资讯，支持按日期过滤；未传日期则默认返回最新的 10 条。
     */
    @Cacheable(cacheNames = "hotNews", key = "#date != null ? #date.toString() : 'latest'")
    @GetMapping
    public ApiResponse<List<HotNews>> list(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date
    ) {
        LambdaQueryWrapper<HotNews> wrapper = new LambdaQueryWrapper<>();
        if (date != null) {
            LocalDateTime start = date.atStartOfDay();
            LocalDateTime end = date.atTime(LocalTime.MAX);
            wrapper.between(HotNews::getPublishDate, start, end);
        }
        wrapper.orderByDesc(HotNews::getPublishDate).last("limit 10");
        List<HotNews> list = hotNewsService.list(wrapper);
        return ApiResponse.ok(list);
    }
}
