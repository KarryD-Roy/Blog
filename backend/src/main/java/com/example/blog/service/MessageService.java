package com.example.blog.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.blog.entity.Message;

public interface MessageService {
    void sendMessage(Message message);
    IPage<Message> getMessagesByUser(Long userId, int page, int size);
    int getUnreadCount(Long userId);
    void markAsRead(Long messageId, Long userId);
    void markAllAsRead(Long userId);
}
