package com.example.blog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.blog.entity.Message;
import com.example.blog.entity.Post;
import com.example.blog.entity.Skill;
import com.example.blog.mapper.PostMapper;
import com.example.blog.mapper.SkillMapper;
import com.example.blog.service.MessageService;
import com.example.blog.service.ReviewService;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReviewServiceImpl implements ReviewService {

    private final PostMapper postMapper;
    private final SkillMapper skillMapper;
    private final RabbitTemplate rabbitTemplate;
    private final MessageService messageService;

    public ReviewServiceImpl(PostMapper postMapper, SkillMapper skillMapper,
                             RabbitTemplate rabbitTemplate, MessageService messageService) {
        this.postMapper = postMapper;
        this.skillMapper = skillMapper;
        this.rabbitTemplate = rabbitTemplate;
        this.messageService = messageService;
    }

    @Override
    @Transactional
    public void reviewPost(Long postId, String status, String reason, Long reviewerId) {
        Post post = postMapper.selectById(postId);
        if (post == null) {
            throw new RuntimeException("文章不存在");
        }

        LambdaUpdateWrapper<Post> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Post::getId, postId)
               .set(Post::getStatus, status)
               .set(Post::getUpdatedAt, java.time.LocalDateTime.now());
        postMapper.update(null, wrapper);

        // Send RabbitMQ notification asynchronously
        Map<String, Object> notification = new HashMap<>();
        notification.put("postId", postId);
        notification.put("title", post.getTitle());
        notification.put("status", status);
        notification.put("reason", reason);
        notification.put("reviewType", "POST");

        try {
            rabbitTemplate.convertAndSend("review.exchange", "review.notification", notification);
        } catch (Exception e) {
            // Fallback: create message directly if RabbitMQ is unavailable
            createNotification(post.getUserId(), post.getTitle(), status, reason, postId);
        }
    }

    @Override
    @Transactional
    public void reviewSkill(Long skillId, String status, String reason, Long reviewerId) {
        Skill skill = skillMapper.selectById(skillId);
        if (skill == null) {
            throw new RuntimeException("技能不存在");
        }

        LambdaUpdateWrapper<Skill> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Skill::getId, skillId)
               .set(Skill::getStatus, status);
        skillMapper.update(null, wrapper);

        Map<String, Object> notification = new HashMap<>();
        notification.put("postId", skillId);
        notification.put("title", skill.getTitle());
        notification.put("status", status);
        notification.put("reason", reason);
        notification.put("reviewType", "SKILL");

        try {
            rabbitTemplate.convertAndSend("review.exchange", "review.notification", notification);
        } catch (Exception e) {
            createNotification(skill.getUserId(), skill.getTitle(), status, reason, skillId);
        }
    }

    @Override
    public List<Post> getPendingPosts() {
        LambdaQueryWrapper<Post> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Post::getStatus, "PENDING")
               .orderByDesc(Post::getCreatedAt);
        return postMapper.selectList(wrapper);
    }

    @Override
    public List<Skill> getPendingSkills() {
        LambdaQueryWrapper<Skill> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Skill::getStatus, "PENDING")
               .orderByAsc(Skill::getId);
        return skillMapper.selectList(wrapper);
    }

    private void createNotification(Long userId, String title, String status, String reason, Long relatedId) {
        if (userId == null) return;
        Message msg = new Message();
        msg.setRecipientId(userId);
        msg.setTitle("审核结果: " + title);
        msg.setContent(status + (reason != null && !reason.isEmpty() ? ": " + reason : ""));
        msg.setType("REVIEW");
        msg.setIsRead(false);
        msg.setRelatedPostId(relatedId);
        messageService.sendMessage(msg);
    }
}
