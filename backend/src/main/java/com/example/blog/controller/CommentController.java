package com.example.blog.controller;

import com.example.blog.dto.CommentRequest;
import com.example.blog.entity.Comment;
import com.example.blog.security.UserContext;
import com.example.blog.service.CommentService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/posts/{postId}/comments")
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @GetMapping
    public ApiResponse<List<Comment>> list(@PathVariable Long postId) {
        return ApiResponse.ok(commentService.getCommentsByPost(postId));
    }

    @PostMapping
    public ApiResponse<Comment> create(@PathVariable Long postId,
                                        @Valid @RequestBody CommentRequest request) {
        Long userId = UserContext.getCurrentUserId();
        if (userId == null) {
            return ApiResponse.error("请先登录");
        }
        Comment comment = commentService.addComment(postId, userId, request.getParentId(), request.getContent());
        return ApiResponse.ok(comment);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long postId, @PathVariable Long id) {
        Long userId = UserContext.getCurrentUserId();
        if (userId == null) {
            return ApiResponse.error("请先登录");
        }
        try {
            commentService.deleteComment(id, userId);
            return ApiResponse.ok(null);
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }
}
