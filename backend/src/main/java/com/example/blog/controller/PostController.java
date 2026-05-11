package com.example.blog.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.blog.config.SearchProperties;
import com.example.blog.entity.Post;
import com.example.blog.search.PostSearchService;
import com.example.blog.service.AiService;
import com.example.blog.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;
    private final PostSearchService postSearchService;
    private final SearchProperties searchProperties;
    private final AiService aiService;

    @Cacheable(cacheNames = "posts:list", key = "'p'+#page+'_'+#size")
    @GetMapping
    public ApiResponse<IPage<Post>> page(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size) {
        LambdaQueryWrapper<Post> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(Post::getCreatedAt);
        IPage<Post> result = postService.page(Page.of(page, size), wrapper);
        return ApiResponse.ok(result);
    }

    /**
     * 文章检索接口（用于搜索和文章大全），优先使用 ES
     */
    @GetMapping("/query")
    public ApiResponse<IPage<Post>> query(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String tag
    ) {
        if (searchProperties.isEnabled()) {
            IPage<Post> result = postSearchService.search(page, size, keyword, categoryId, tag);
            return ApiResponse.ok(result);
        }

        LambdaQueryWrapper<Post> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w
                    .like(Post::getTitle, keyword)
                    .or()
                    .like(Post::getSummary, keyword)
                    .or()
                    .like(Post::getContent, keyword)
                    .or()
                    .like(Post::getTags, keyword)
            );
        }
        if (categoryId != null) {
            wrapper.eq(Post::getCategoryId, categoryId);
        }
        if (StringUtils.hasText(tag)) {
            String trimmed = tag.trim();
            wrapper.like(Post::getTags, trimmed);
        }
        wrapper.orderByDesc(Post::getPinned).orderByDesc(Post::getCreatedAt);
        IPage<Post> result = postService.page(Page.of(page, size), wrapper);
        return ApiResponse.ok(result);
    }

    /**
     * 单独更新文章置顶状态
     */
    @CacheEvict(cacheNames = "posts:list", allEntries = true)
    @PutMapping("/{id}/pin")
    public ApiResponse<Post> updatePin(@PathVariable Long id, @RequestParam boolean pinned) {
        Post post = postService.getById(id);
        if (post == null) {
            return ApiResponse.error("文章不存在");
        }
        post.setPinned(pinned);
        post.setUpdatedAt(LocalDateTime.now());
        postService.updateById(post);
        postSearchService.index(post);
        return ApiResponse.ok(post);
    }

    /**
     * 返回当前系统中所有去重后的标签列表，用于前端标签快捷搜索
     */
    @GetMapping("/tags")
    public ApiResponse<Set<String>> allTags() {
        List<Post> allPosts = postService.list();
        Set<String> tags = allPosts.stream()
                .map(Post::getTags)
                .filter(StringUtils::hasText)
                .flatMap(s -> Arrays.stream(s.split(",")))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet());
        return ApiResponse.ok(tags);
    }

    @CacheEvict(cacheNames = "posts:list", allEntries = true)
    @GetMapping("/{id}")
    public ApiResponse<Post> detail(@PathVariable Long id) {
        Post post = null;
        if (searchProperties.isEnabled()) {
            post = postSearchService.getById(id);
        }

        if (post == null) {
            post = postService.getById(id);
        }

        if (post == null) {
            return ApiResponse.error("文章不存在");
        }

        Post updated = postService.incrementViewCount(id);
        if (updated != null) {
            post.setViewCount(updated.getViewCount());
        }

        if (searchProperties.isEnabled()) {
            postSearchService.index(post);
        }
        return ApiResponse.ok(post);
    }

    @CacheEvict(cacheNames = "posts:list", allEntries = true)
    @PostMapping
    public ApiResponse<Post> create(@RequestBody Post post) {
        post.setId(null);
        LocalDateTime now = LocalDateTime.now();
        post.setCreatedAt(now);
        post.setUpdatedAt(now);
        post.setViewCount(0);
        postService.save(post);
        postSearchService.index(post);
        // Asynchronously ingest into Vector DB
        try {
             aiService.ingestArticle(post.getId());
        } catch (Exception e) {
            // Log but don't fail the request
            e.printStackTrace();
        }
        return ApiResponse.ok(post);
    }

    @CacheEvict(cacheNames = "posts:list", allEntries = true)
    @PutMapping("/{id}")
    public ApiResponse<Post> update(@PathVariable Long id, @RequestBody Post post) {
        post.setId(id);
        post.setUpdatedAt(LocalDateTime.now());
        postService.updateById(post);
        postSearchService.index(post);
         // Asynchronously ingest into Vector DB
        try {
             aiService.ingestArticle(post.getId());
        } catch (Exception e) {
            // Log but don't fail the request
             e.printStackTrace();
        }
        return ApiResponse.ok(post);
    }

    @CacheEvict(cacheNames = "posts:list", allEntries = true)
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        postService.removeById(id);
        postSearchService.delete(id);
        // Asynchronously remove from Vector DB
        try {
            aiService.deleteArticle(id);
        } catch (Exception e) {
            // Log but don't fail the request
            e.printStackTrace();
        }
        return ApiResponse.ok(null);
    }

    @PostMapping("/reindex")
    public ApiResponse<Void> reindex() {
        postSearchService.reindexAll();
        return ApiResponse.ok(null);
    }
}
