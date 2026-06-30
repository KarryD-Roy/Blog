package com.example.blog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.blog.entity.Comment;
import com.example.blog.mapper.CommentMapper;
import com.example.blog.security.UserContext;
import com.example.blog.service.CommentService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CommentServiceImpl implements CommentService {

    private final CommentMapper commentMapper;

    public CommentServiceImpl(CommentMapper commentMapper) {
        this.commentMapper = commentMapper;
    }

    @Override
    @Transactional
    public Comment addComment(Long postId, Long userId, Long parentId, String content) {
        Comment comment = new Comment();
        comment.setPostId(postId);
        comment.setUserId(userId);
        comment.setParentId(parentId);
        comment.setContent(content);
        comment.setCreatedAt(LocalDateTime.now());
        commentMapper.insert(comment);
        return comment;
    }

    @Override
    public List<Comment> getCommentsByPost(Long postId) {
        LambdaQueryWrapper<Comment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Comment::getPostId, postId)
               .orderByAsc(Comment::getCreatedAt);
        return commentMapper.selectList(wrapper);
    }

    @Override
    @Transactional
    public void deleteComment(Long commentId, Long userId) {
        Comment comment = commentMapper.selectById(commentId);
        if (comment == null) {
            throw new RuntimeException("评论不存在");
        }

        Long currentUserId = userId != null ? userId : UserContext.getCurrentUserId();
        boolean isOwner = currentUserId != null && currentUserId.equals(comment.getUserId());
        boolean isAdmin = currentUserId != null; // simplified, ideally check roles

        if (!isOwner && !isAdmin) {
            throw new RuntimeException("无权删除该评论");
        }

        commentMapper.deleteById(commentId);
    }
}
