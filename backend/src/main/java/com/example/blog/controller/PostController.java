package com.example.blog.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.blog.entity.Post;
import com.example.blog.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    @GetMapping
    public ApiResponse<IPage<Post>> page(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size) {
        IPage<Post> result = postService.page(Page.of(page, size));
        return ApiResponse.ok(result);
    }

    @GetMapping("/{id}")
    public ApiResponse<Post> detail(@PathVariable Long id) {
        Post post = postService.getById(id);
        if (post == null) {
            return ApiResponse.error("文章不存在");
        }
        post.setViewCount(post.getViewCount() == null ? 1 : post.getViewCount() + 1);
        postService.updateById(post);
        return ApiResponse.ok(post);
    }

    @PostMapping
    public ApiResponse<Post> create(@RequestBody Post post) {
        post.setId(null);
        LocalDateTime now = LocalDateTime.now();
        post.setCreatedAt(now);
        post.setUpdatedAt(now);
        post.setViewCount(0);
        postService.save(post);
        return ApiResponse.ok(post);
    }

    @PutMapping("/{id}")
    public ApiResponse<Post> update(@PathVariable Long id, @RequestBody Post post) {
        post.setId(id);
        post.setUpdatedAt(LocalDateTime.now());
        postService.updateById(post);
        return ApiResponse.ok(post);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        postService.removeById(id);
        return ApiResponse.ok(null);
    }
}

