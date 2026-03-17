package com.example.blog.controller;

import com.example.blog.entity.Category;
import com.example.blog.entity.Post;
import com.example.blog.service.CategoryService;
import com.example.blog.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/statistics")
@RequiredArgsConstructor
public class StatisticsController {

    private final PostService postService;
    private final CategoryService categoryService;

    @GetMapping("/categories")
    public ApiResponse<List<Map<String, Object>>> categoryStats() {
        List<Post> posts = postService.list();
        Map<Long, Long> counts = posts.stream()
                .filter(p -> p.getCategoryId() != null)
                .collect(Collectors.groupingBy(Post::getCategoryId, Collectors.counting()));

        List<Category> categories = categoryService.list();
        List<Map<String, Object>> result = new ArrayList<>();

        for (Category category : categories) {
            Map<String, Object> map = new HashMap<>();
            map.put("name", category.getName());
            map.put("value", counts.getOrDefault(category.getId(), 0L));
            result.add(map);
        }

        long uncategorizedCount = posts.stream().filter(p -> p.getCategoryId() == null).count();
        if (uncategorizedCount > 0) {
            Map<String, Object> map = new HashMap<>();
            map.put("name", "未分类");
            map.put("value", uncategorizedCount);
            result.add(map);
        }

        return ApiResponse.ok(result);
    }

    @GetMapping("/tags")
    public ApiResponse<List<Map<String, Object>>> tagStats() {
        List<Post> posts = postService.list();
        Map<String, Long> tagCounts = new HashMap<>();

        for (Post post : posts) {
            if (StringUtils.hasText(post.getTags())) {
                String[] tags = post.getTags().split(",");
                for (String tag : tags) {
                    String t = tag.trim();
                    if (!t.isEmpty()) {
                        tagCounts.merge(t, 1L, Long::sum);
                    }
                }
            }
        }

        List<Map<String, Object>> result = tagCounts.entrySet().stream()
                .map(entry -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("name", entry.getKey());
                    map.put("value", entry.getValue());
                    return map;
                })
                .sorted((a, b) -> Long.compare((Long) b.get("value"), (Long) a.get("value")))
                .collect(Collectors.toList());

        return ApiResponse.ok(result);
    }

    @GetMapping("/views")
    public ApiResponse<List<Map<String, Object>>> viewStats() {
        List<Post> posts = postService.list();

        List<Map<String, Object>> result = posts.stream()
                .sorted(Comparator.comparing((Post p) -> Optional.ofNullable(p.getViewCount()).orElse(0)).reversed())
                .limit(10)
                .map(p -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("name", p.getTitle());
                    map.put("value", Optional.ofNullable(p.getViewCount()).orElse(0));
                    return map;
                })
                .collect(Collectors.toList());

        return ApiResponse.ok(result);
    }

    @GetMapping("/timeline")
    public ApiResponse<List<Map<String, Object>>> timelineStats() {
        List<Post> posts = postService.list();

        Map<LocalDate, Long> counts = posts.stream()
                .filter(p -> p.getCreatedAt() != null)
                .collect(Collectors.groupingBy(p -> p.getCreatedAt().toLocalDate(), Collectors.counting()));

        LocalDate today = LocalDate.now();
        List<Map<String, Object>> result = new ArrayList<>();

        for (int i = 13; i >= 0; i--) {
            LocalDate day = today.minusDays(i);
            Map<String, Object> map = new HashMap<>();
            map.put("date", day.toString());
            map.put("value", counts.getOrDefault(day, 0L));
            result.add(map);
        }

        return ApiResponse.ok(result);
    }
}
