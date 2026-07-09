package com.example.blog.service;

import com.example.blog.dto.CommentVo;
import com.example.blog.entity.Comment;

import java.util.List;

public interface CommentService {
    Comment addComment(Long postId, Long userId, Long parentId, String content);

    List<CommentVo> getCommentsByPost(Long postId);

    void deleteComment(Long commentId, Long userId);
}
