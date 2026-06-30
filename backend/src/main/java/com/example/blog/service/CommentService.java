package com.example.blog.service;

import com.example.blog.entity.Comment;

import java.util.List;

public interface CommentService {
    Comment addComment(Long postId, Long userId, Long parentId, String content);
    List<Comment> getCommentsByPost(Long postId);
    void deleteComment(Long commentId, Long userId);
}
