package com.example.blog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.blog.dto.CommentVo;
import com.example.blog.entity.Comment;
import com.example.blog.entity.User;
import com.example.blog.mapper.CommentMapper;
import com.example.blog.mapper.UserMapper;
import com.example.blog.security.UserContext;
import com.example.blog.service.CommentService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CommentServiceImpl implements CommentService {

    private final CommentMapper commentMapper;
    private final UserMapper userMapper;

    public CommentServiceImpl(CommentMapper commentMapper, UserMapper userMapper) {
        this.commentMapper = commentMapper;
        this.userMapper = userMapper;
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
    public List<CommentVo> getCommentsByPost(Long postId) {
        LambdaQueryWrapper<Comment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Comment::getPostId, postId)
               .orderByAsc(Comment::getCreatedAt);
        List<Comment> comments = commentMapper.selectList(wrapper);

        if (comments.isEmpty()) {
            return List.of();
        }

        // 批量查询评论作者的真实用户名/昵称
        Set<Long> userIds = comments.stream()
                .map(Comment::getUserId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        List<User> users = userMapper.selectBatchIds(userIds);
        Map<Long, User> userMap = users.stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        return comments.stream().map(c -> {
            User author = userMap.get(c.getUserId());
            CommentVo vo = new CommentVo();
            vo.setId(c.getId());
            vo.setPostId(c.getPostId());
            vo.setUserId(c.getUserId());
            vo.setParentId(c.getParentId());
            vo.setContent(c.getContent());
            vo.setCreatedAt(c.getCreatedAt());
            vo.setUsername(author != null ? author.getUsername() : null);
            vo.setNickname(author != null ? author.getNickname() : null);
            return vo;
        }).collect(Collectors.toList());
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
