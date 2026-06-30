package com.example.blog.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.blog.entity.Message;
import com.example.blog.security.UserContext;
import com.example.blog.service.MessageService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/messages")
public class MessageController {

    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    @GetMapping
    public ApiResponse<IPage<Message>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        Long userId = UserContext.getCurrentUserId();
        if (userId == null) {
            return ApiResponse.error("请先登录");
        }
        return ApiResponse.ok(messageService.getMessagesByUser(userId, page, size));
    }

    @GetMapping("/unread-count")
    public ApiResponse<Integer> unreadCount() {
        Long userId = UserContext.getCurrentUserId();
        if (userId == null) {
            return ApiResponse.error("请先登录");
        }
        return ApiResponse.ok(messageService.getUnreadCount(userId));
    }

    @PutMapping("/{id}/read")
    public ApiResponse<Void> markAsRead(@PathVariable Long id) {
        Long userId = UserContext.getCurrentUserId();
        if (userId == null) {
            return ApiResponse.error("请先登录");
        }
        messageService.markAsRead(id, userId);
        return ApiResponse.ok(null);
    }

    @PutMapping("/read-all")
    public ApiResponse<Void> markAllAsRead() {
        Long userId = UserContext.getCurrentUserId();
        if (userId == null) {
            return ApiResponse.error("请先登录");
        }
        messageService.markAllAsRead(userId);
        return ApiResponse.ok(null);
    }
}
