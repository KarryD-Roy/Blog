package com.example.blog.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.blog.config.SearchProperties;
import com.example.blog.dto.UserProfileVo;
import com.example.blog.entity.Message;
import com.example.blog.entity.Post;
import com.example.blog.search.PostSearchService;
import com.example.blog.security.UserContext;
import com.example.blog.service.MessageService;
import com.example.blog.service.PostService;
import com.example.blog.service.KnowledgeBaseService;
import com.example.blog.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
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
    private final KnowledgeBaseService knowledgeBaseService;
    private final UserService userService;
    private final MessageService messageService;

    @Cacheable(cacheNames = "posts:list", key = "'p'+#page+'_'+#size+'_a'+#authorId")
    @GetMapping
    public ApiResponse<IPage<Post>> page(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) Long authorId) {
        LambdaQueryWrapper<Post> wrapper = new LambdaQueryWrapper<>();
        // 公开列表只展示已发布的文章，不展示待审核/已驳回的
        wrapper.eq(Post::getStatus, "PUBLISHED");
        if (authorId != null) {
            wrapper.eq(Post::getUserId, authorId);
        }
        wrapper.orderByDesc(Post::getPinned)
               .orderByDesc(Post::getCreatedAt);
        IPage<Post> result = postService.page(Page.of(page, size), wrapper);

        // Populate skillIds
        if (result.getRecords() != null) {
            for (Post post : result.getRecords()) {
                post.setSkillIds(postService.getSkillIdsByPostId(post.getId()));
            }
        }
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
        // 公开查询只展示已发布的文章
        wrapper.eq(Post::getStatus, "PUBLISHED");
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

        // Populate skillIds
        if (result.getRecords() != null) {
            for (Post post : result.getRecords()) {
                post.setSkillIds(postService.getSkillIdsByPostId(post.getId()));
            }
        }
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
        if (!canModify(post)) {
            return ApiResponse.error("无权操作该文章，仅作者可修改置顶状态");
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

        List<Long> skillIds = postService.getSkillIdsByPostId(id);
        post.setSkillIds(skillIds);

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
        Long userId = UserContext.getCurrentUserId();
        if (userId == null) {
            return ApiResponse.error("请先登录");
        }
        post.setId(null);
        post.setUserId(userId);
        // 普通用户提交的文章默认为待审核状态，ADMIN角色直接发布
        boolean isAdmin = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        post.setStatus(isAdmin ? "PUBLISHED" : "PENDING");
        LocalDateTime now = LocalDateTime.now();
        post.setCreatedAt(now);
        post.setUpdatedAt(now);
        post.setViewCount(0);
        postService.savePostWithSkills(post);
        postSearchService.index(post);

        // 普通用户提交的文章进入待审核状态后，自动通知所有管理员及时处理
        if ("PENDING".equals(post.getStatus())) {
            notifyAdminsOfPendingPost(post);
        }

        try {
             knowledgeBaseService.syncArticle(post.getId());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ApiResponse.ok(post);
    }

    private void notifyAdminsOfPendingPost(Post post) {
        try {
            List<UserProfileVo> allUsers = userService.getAllUsers();
            for (UserProfileVo user : allUsers) {
                if (user.getRoles() != null && user.getRoles().contains("ADMIN")) {
                    Message msg = new Message();
                    msg.setRecipientId(user.getId());
                    msg.setSenderId(post.getUserId());
                    msg.setTitle("新文章待审核");
                    msg.setContent("用户提交了待审核文章《" + post.getTitle() + "》，请及时处理。");
                    msg.setType("REVIEW");
                    msg.setIsRead(false);
                    msg.setRelatedPostId(post.getId());
                    messageService.sendMessage(msg);
                }
            }
        } catch (Exception e) {
            // 通知发送失败不应影响文章发布主流程
            e.printStackTrace();
        }
    }

    @CacheEvict(cacheNames = "posts:list", allEntries = true)
    @PutMapping("/{id}")
    public ApiResponse<Post> update(@PathVariable Long id, @RequestBody Post post) {
        Post existing = postService.getById(id);
        if (existing == null) {
            return ApiResponse.error("文章不存在");
        }
        if (!canModify(existing)) {
            return ApiResponse.error("无权修改该文章");
        }
        post.setId(id);
        post.setUpdatedAt(LocalDateTime.now());
        postService.updatePostWithSkills(post);
        postSearchService.index(post);
        try {
             knowledgeBaseService.syncArticle(post.getId());
        } catch (Exception e) {
             e.printStackTrace();
        }
        return ApiResponse.ok(post);
    }

    @CacheEvict(cacheNames = "posts:list", allEntries = true)
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        Post existing = postService.getById(id);
        if (existing == null) {
            return ApiResponse.error("文章不存在");
        }
        if (!canModify(existing)) {
            return ApiResponse.error("无权删除该文章");
        }
        postService.removeById(id);
        postSearchService.delete(id);
        return ApiResponse.ok(null);
    }

    /**
     * 权限校验：仅允许文章作者本人执行修改/删除操作
     * 管理员也不得越权操作他人的文章，确保数据安全
     */
    private boolean canModify(Post post) {
        Long userId = UserContext.getCurrentUserId();
        if (userId == null) return false;
        return userId.equals(post.getUserId());
    }

    @PostMapping("/reindex")
    public ApiResponse<Void> reindex() {
        postSearchService.reindexAll();
        return ApiResponse.ok(null);
    }
}
