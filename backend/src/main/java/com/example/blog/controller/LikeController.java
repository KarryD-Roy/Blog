package com.example.blog.controller;

import com.example.blog.security.UserContext;
import com.example.blog.service.LikeService;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/posts/{postId}/like")
public class LikeController {

    private final LikeService likeService;

    public LikeController(LikeService likeService) {
        this.likeService = likeService;
    }

    @PostMapping
    public ApiResponse<Map<String, Object>> toggle(@PathVariable Long postId) {
        Long userId = UserContext.getCurrentUserId();
        if (userId == null) {
            return ApiResponse.error("请先登录");
        }
        boolean liked = likeService.toggleLike(postId, userId);
        long count = likeService.getLikeCount(postId);
        Map<String, Object> result = new HashMap<>();
        result.put("liked", liked);
        result.put("likeCount", count);
        return ApiResponse.ok(result);
    }

    @GetMapping
    public ApiResponse<Map<String, Object>> status(@PathVariable Long postId) {
        Long userId = UserContext.getCurrentUserId();
        long count = likeService.getLikeCount(postId);
        boolean liked = userId != null && likeService.isLiked(postId, userId);
        Map<String, Object> result = new HashMap<>();
        result.put("liked", liked);
        result.put("likeCount", count);
        return ApiResponse.ok(result);
    }
}
