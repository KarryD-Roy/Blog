package com.example.blog.service;

public interface LikeService {
    boolean toggleLike(Long postId, Long userId);
    boolean isLiked(Long postId, Long userId);
    long getLikeCount(Long postId);
}
