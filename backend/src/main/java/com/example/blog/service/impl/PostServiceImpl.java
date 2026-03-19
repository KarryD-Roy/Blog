package com.example.blog.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.blog.entity.Post;
import com.example.blog.mapper.PostMapper;
import com.example.blog.service.PostService;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;

@Service
public class PostServiceImpl extends ServiceImpl<PostMapper, Post> implements PostService {
    // 文章浏览量自增，使用 SQL 方式避免并发问题
    @Override
    public Post incrementViewCount(Long id) {
        LambdaUpdateWrapper<Post> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Post::getId, id).setSql("view_count = COALESCE(view_count, 0) + 1");
        int updated = this.baseMapper.update(null, updateWrapper);
        if (updated > 0) {
            return this.getById(id);
        }
        return null;
    }
}
