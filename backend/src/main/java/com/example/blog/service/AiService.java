package com.example.blog.service;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.util.Map;
import java.util.List;

public interface AiService {
    SseEmitter summarizeArticle(Long articleId);
    SseEmitter generateDraft(List<String> materials, String style);
    Map<String, Object> getRecommendations(String query);
    void ingestArticle(Long articleId);
}
