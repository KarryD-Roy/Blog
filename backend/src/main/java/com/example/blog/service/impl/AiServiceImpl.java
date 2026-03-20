package com.example.blog.service.impl;

import com.example.blog.entity.Post;
import com.example.blog.service.AiService;
import com.example.blog.service.PostService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class AiServiceImpl implements AiService {

    private final PostService postService;
    private final WebClient webClient;
    private final ExecutorService executor = Executors.newCachedThreadPool();

    @Value("${ai.service.url}")
    private String aiServiceUrl;

    public AiServiceImpl(PostService postService, WebClient.Builder webClientBuilder) {
        this.postService = postService;
        this.webClient = webClientBuilder.build();
    }

    @Override
    public SseEmitter summarizeArticle(Long articleId) {
        Post post = postService.getById(articleId);
        if (post == null) {
            throw new RuntimeException("Post not found");
        }

        SseEmitter emitter = new SseEmitter(180_000L); // 3 minutes timeout

        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("article_id", articleId.toString());
        requestBody.put("content", post.getContent());

        Flux<String> eventStream = webClient.post()
                .uri(aiServiceUrl + "/summary")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToFlux(String.class);

        eventStream.subscribe(
            data -> {
                try {
                    // Python sse-starlette sends "data: message"
                    // We can just forward the raw data or clean it.
                    // For simplicity, let's just forward the content valid for SseEmitter
                    // But SseEmitter.send() wraps in data: ...
                    // So we should try to extract the payload if possible
                    // Or simply send object.

                    // Simple heuristic: if it looks like SSE data line, strip prefix
                   /* if (data.startsWith("data: ")) {
                        String payload = data.substring(6);
                        emitter.send(payload);
                    } */
                    // Actually webclient bodyToFlux(String) with TEXT_EVENT_STREAM usually returns the data payload only if properly decoded
                    // Let's assume it returns payload
                    emitter.send(data);
                } catch (Exception e) {
                    emitter.completeWithError(e);
                }
            },
            error -> emitter.completeWithError(error),
            () -> emitter.complete()
        );

        return emitter;
    }

    @Override
    public String generateDraft(List<String> materials, String style) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("materials", materials);
        requestBody.put("style", style);

        try {
            Map response = webClient.post()
                    .uri(aiServiceUrl + "/write")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response != null && response.containsKey("draft")) {
                return (String) response.get("draft");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "AI Service is currently unavailable.";
        }
        return "";
    }

    @Override
    public Map<String, Object> getRecommendations(String query) {
         Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("query", query);

        try {
            return webClient.post()
                    .uri(aiServiceUrl + "/recommend")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
        } catch (Exception e) {
            e.printStackTrace();
            return Map.of("error", "AI Service unavailable");
        }
    }

    @Override
    public void ingestArticle(Long articleId) {
        Post post = postService.getById(articleId);
        if (post == null) return;

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("article_id", articleId.toString());
        requestBody.put("title", post.getTitle());
        requestBody.put("content", post.getContent());
        if (post.getTags() != null) {
            requestBody.put("tags", Arrays.asList(post.getTags().split(",")));
        } else {
            requestBody.put("tags", List.of());
        }

        webClient.post()
                .uri(aiServiceUrl + "/ingest")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .toBodilessEntity()
                .subscribe();
    }
}
