package com.example.blog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.blog.entity.Message;
import com.example.blog.mapper.MessageMapper;
import com.example.blog.service.MessageService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class MessageServiceImpl implements MessageService {

    private final MessageMapper messageMapper;

    public MessageServiceImpl(MessageMapper messageMapper) {
        this.messageMapper = messageMapper;
    }

    @Override
    @Transactional
    public void sendMessage(Message message) {
        if (message.getCreatedAt() == null) {
            message.setCreatedAt(LocalDateTime.now());
        }
        if (message.getIsRead() == null) {
            message.setIsRead(false);
        }
        if (message.getType() == null) {
            message.setType("SYSTEM");
        }
        messageMapper.insert(message);
    }

    @Override
    public IPage<Message> getMessagesByUser(Long userId, int page, int size) {
        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Message::getRecipientId, userId)
               .orderByDesc(Message::getCreatedAt);
        return messageMapper.selectPage(Page.of(page, size), wrapper);
    }

    @Override
    public int getUnreadCount(Long userId) {
        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Message::getRecipientId, userId)
               .eq(Message::getIsRead, false);
        Long count = messageMapper.selectCount(wrapper);
        return count != null ? count.intValue() : 0;
    }

    @Override
    @Transactional
    public void markAsRead(Long messageId, Long userId) {
        LambdaUpdateWrapper<Message> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Message::getId, messageId)
               .eq(Message::getRecipientId, userId)
               .set(Message::getIsRead, true);
        messageMapper.update(null, wrapper);
    }

    @Override
    @Transactional
    public void markAllAsRead(Long userId) {
        LambdaUpdateWrapper<Message> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Message::getRecipientId, userId)
               .eq(Message::getIsRead, false)
               .set(Message::getIsRead, true);
        messageMapper.update(null, wrapper);
    }
}
