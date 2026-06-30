package com.example.blog.controller;

import com.example.blog.entity.Post;
import com.example.blog.entity.Skill;
import com.example.blog.security.UserContext;
import com.example.blog.service.ReviewService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/review")
@PreAuthorize("hasRole('ADMIN')")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping("/posts")
    public ApiResponse<List<Post>> pendingPosts() {
        return ApiResponse.ok(reviewService.getPendingPosts());
    }

    @GetMapping("/skills")
    public ApiResponse<List<Skill>> pendingSkills() {
        return ApiResponse.ok(reviewService.getPendingSkills());
    }

    @PutMapping("/posts/{id}")
    public ApiResponse<Void> reviewPost(@PathVariable Long id, @RequestBody Map<String, String> body) {
        Long reviewerId = UserContext.getCurrentUserId();
        if (reviewerId == null) {
            return ApiResponse.error("未登录");
        }
        String status = body.getOrDefault("status", "APPROVED");
        String reason = body.getOrDefault("reason", "");
        reviewService.reviewPost(id, status, reason, reviewerId);
        return ApiResponse.ok(null);
    }

    @PutMapping("/skills/{id}")
    public ApiResponse<Void> reviewSkill(@PathVariable Long id, @RequestBody Map<String, String> body) {
        Long reviewerId = UserContext.getCurrentUserId();
        if (reviewerId == null) {
            return ApiResponse.error("未登录");
        }
        String status = body.getOrDefault("status", "APPROVED");
        String reason = body.getOrDefault("reason", "");
        reviewService.reviewSkill(id, status, reason, reviewerId);
        return ApiResponse.ok(null);
    }
}
