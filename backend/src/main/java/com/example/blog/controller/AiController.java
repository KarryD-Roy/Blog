package com.example.blog.controller;

import com.example.blog.service.AiService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    @Autowired
    private AiService aiService;

    @GetMapping("/summary/{articleId}")
    public SseEmitter summarizeArticle(@PathVariable Long articleId) {
        return aiService.summarizeArticle(articleId);
    }

    @PostMapping("/write")
    public SseEmitter generateDraft(@RequestBody Map<String, Object> request) {
        List<String> materials = (List<String>) request.get("materials");
        String style = (String) request.getOrDefault("style", "technical");
        return aiService.generateDraft(materials, style);
    }

    @PostMapping("/recommend")
    public ResponseEntity<ApiResponse<Map<String, Object>>> recommend(@RequestBody Map<String, String> request) {
        String query = request.get("query");
        Map<String, Object> recommendations = aiService.getRecommendations(query);
        return ResponseEntity.ok(ApiResponse.ok(recommendations));
    }
}
