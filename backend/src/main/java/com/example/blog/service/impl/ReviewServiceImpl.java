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

        // 审核通过统一以 PUBLISHED 落地，否则文章虽通过审核却无法进入公开列表
        String targetStatus = "APPROVED".equalsIgnoreCase(status) ? "PUBLISHED" : status;

        LambdaUpdateWrapper<Post> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Post::getId, postId)
               .set(Post::getStatus, targetStatus)
               .set(Post::getUpdatedAt, java.time.LocalDateTime.now());
        postMapper.update(null, wrapper);

        // 始终直接创建消息通知（确保作者必收到，不依赖 RabbitMQ 可用性）
        createNotification(post.getUserId(), post.getTitle(), status, reason, postId, "POST");

        // 异步尝试通过 RabbitMQ 发送（用于扩展场景如邮件推送等），失败不主流程
        try {
            Map<String, Object> notification = new HashMap<>();
            notification.put("postId", postId);
            notification.put("title", post.getTitle());
            notification.put("status", status);
            notification.put("reason", reason);
            notification.put("reviewType", "POST");
            rabbitTemplate.convertAndSend("review.exchange", "review.notification", notification);
        } catch (Exception e) {
            // RabbitMQ 不可用时静默忽略，已通过上面的直接写入保证通知送达
        }
    }

    @Override
    @Transactional
    public void reviewSkill(Long skillId, String status, String reason, Long reviewerId) {
        Skill skill = skillMapper.selectById(skillId);
        if (skill == null) {
            throw new RuntimeException("技能不存在");
        }

        // 审核通过统一以 PUBLISHED 落地，与文章审核保持一致
        String targetStatus = "APPROVED".equalsIgnoreCase(status) ? "PUBLISHED" : status;

        LambdaUpdateWrapper<Skill> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Skill::getId, skillId)
               .set(Skill::getStatus, targetStatus);
        skillMapper.update(null, wrapper);

        // 始终直接创建消息通知（确保作者必收到）
        createNotification(skill.getUserId(), skill.getTitle(), status, reason, skillId, "SKILL");

        // 异步尝试通过 RabbitMQ 发送（扩展场景），失败不影响主流程
        try {
            Map<String, Object> notification = new HashMap<>();
            notification.put("postId", skillId);
            notification.put("title", skill.getTitle());
            notification.put("status", status);
            notification.put("reason", reason);
            notification.put("reviewType", "SKILL");
            rabbitTemplate.convertAndSend("review.exchange", "review.notification", notification);
        } catch (Exception e) {
            // 已通过上面的直接写入保证通知送达
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

    private void createNotification(Long userId, String title, String status, String reason, Long relatedId, String reviewType) {
        if (userId == null) return;
        Message msg = new Message();
        msg.setRecipientId(userId);
        msg.setRelatedPostId(relatedId);
        msg.setType("REVIEW");
        msg.setIsRead(false);

        String typeName = "SKILL".equalsIgnoreCase(reviewType) ? "技能" : "文章";
        String typeTag = "SKILL".equalsIgnoreCase(reviewType) ? "SKILL" : "POST";
        String cleanReason = (reason != null && !reason.trim().isEmpty()) ? reason.trim() : null;

        if ("APPROVED".equalsIgnoreCase(status)) {
            msg.setTitle("[" + typeTag + "] 审核通过：" + typeName + "《" + title + "》");
            msg.setContent("您好，您的" + typeName + "《" + title + "》已通过管理员审核，现已正式发布。感谢您对社区优质内容的贡献。");
        } else if ("REJECTED".equalsIgnoreCase(status)) {
            msg.setTitle("[" + typeTag + "] 审核未通过：" + typeName + "《" + title + "》");
            StringBuilder content = new StringBuilder();
            content.append("您好，您的").append(typeName).append("《").append(title).append("》未能通过审核。");
            if (cleanReason != null) {
                content.append("未通过原因：").append(cleanReason).append("。");
            }
            content.append("建议您根据审核意见修改后重新提交，如有疑问可联系管理员。");
            msg.setContent(content.toString());
        } else {
            msg.setTitle("[" + typeTag + "] 审核状态更新：" + typeName + "《" + title + "》");
            StringBuilder content = new StringBuilder();
            content.append("您好，您的").append(typeName).append("《").append(title).append("》的审核状态已更新为：").append(status).append("。");
            if (cleanReason != null) {
                content.append("补充说明：").append(cleanReason).append("。");
            }
            content.append("请登录个人中心查看详情。");
            msg.setContent(content.toString());
        }

        messageService.sendMessage(msg);
    }
}
