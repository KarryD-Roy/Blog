package com.example.blog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.blog.entity.PostLike;
import com.example.blog.mapper.PostLikeMapper;
import com.example.blog.service.LikeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class LikeServiceImpl implements LikeService {

    private final PostLikeMapper postLikeMapper;

    public LikeServiceImpl(PostLikeMapper postLikeMapper) {
        this.postLikeMapper = postLikeMapper;
    }

    @Override
    @Transactional
    public boolean toggleLike(Long postId, Long userId) {
        LambdaQueryWrapper<PostLike> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PostLike::getPostId, postId)
               .eq(PostLike::getUserId, userId);
        PostLike existing = postLikeMapper.selectOne(wrapper);

        if (existing != null) {
            postLikeMapper.deleteById(existing.getId());
            return false; // unliked
        }

        PostLike like = new PostLike();
        like.setPostId(postId);
        like.setUserId(userId);
        like.setCreatedAt(LocalDateTime.now());
        postLikeMapper.insert(like);
        return true; // liked
    }

    @Override
    public boolean isLiked(Long postId, Long userId) {
        if (userId == null) return false;
        LambdaQueryWrapper<PostLike> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PostLike::getPostId, postId)
               .eq(PostLike::getUserId, userId);
        return postLikeMapper.selectCount(wrapper) > 0;
    }

    @Override
    public long getLikeCount(Long postId) {
        LambdaQueryWrapper<PostLike> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PostLike::getPostId, postId);
        return postLikeMapper.selectCount(wrapper);
    }
}
