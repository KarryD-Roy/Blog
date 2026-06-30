package com.example.blog.config;

import com.example.blog.entity.Message;
import com.example.blog.service.MessageService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class NotificationListener {

    private final MessageService messageService;

    public NotificationListener(MessageService messageService) {
        this.messageService = messageService;
    }

    @RabbitListener(queues = RabbitMqConfig.REVIEW_QUEUE)
    public void handleReviewNotification(Map<String, Object> notification) {
        try {
            Long postId = notification.get("postId") != null
                    ? Long.valueOf(notification.get("postId").toString()) : null;
            String title = (String) notification.getOrDefault("title", "内容");
            String status = (String) notification.getOrDefault("status", "UNKNOWN");
            String reason = (String) notification.getOrDefault("reason", "");
            String reviewType = (String) notification.getOrDefault("reviewType", "POST");

            String messageContent = "审核状态: " + status;
            if (reason != null && !reason.isEmpty()) {
                messageContent += "，原因: " + reason;
            }

            // Find the owner of the reviewed item
            // For simplicity, we use a system sender (null senderId)
            Message msg = new Message();
            msg.setRecipientId(1L); // Default to admin, in production fetch from post/skill userId
            msg.setSenderId(null);
            msg.setTitle("[" + reviewType + "] 审核结果: " + title);
            msg.setContent(messageContent);
            msg.setType("REVIEW");
            msg.setIsRead(false);
            msg.setRelatedPostId(postId);

            messageService.sendMessage(msg);
        } catch (Exception e) {
            System.err.println("Failed to process notification: " + e.getMessage());
        }
    }
}
