package com.example.blog.service.impl;

import com.example.blog.entity.Post;
import com.example.blog.service.AiService;
import com.example.blog.service.PostService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AiServiceImpl implements AiService {

    private final PostService postService;
    private final WebClient webClient;

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

        Flux<ServerSentEvent<String>> eventStream = webClient.post()
                .uri(aiServiceUrl + "/summary")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToFlux(new ParameterizedTypeReference<ServerSentEvent<String>>() {});

        eventStream.subscribe(
            content -> {
                try {
                    String data = content.data();
                    if (data != null) {
                        emitter.send(data);
                    }
                } catch (Exception e) {
                    emitter.completeWithError(e);
                }
            },
            emitter::completeWithError,
            emitter::complete
        );

        return emitter;
    }

    @Override
    public SseEmitter generateDraft(List<String> materials, String style) {
        SseEmitter emitter = new SseEmitter(300_000L); // 5 minutes timeout

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("materials", materials);
        requestBody.put("style", style);

        Flux<ServerSentEvent<String>> eventStream = webClient.post()
                .uri(aiServiceUrl + "/write")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToFlux(new ParameterizedTypeReference<ServerSentEvent<String>>() {});

        eventStream.subscribe(
            content -> {
                try {
                    String data = content.data();
                    if (data != null) {
                        // Avoid double wrapping if SseEmitter adds "data:" prefix
                        // SseEmitter.send(object) formats as "data:object\n\n"
                        // Our frontend expects "data: <content>"
                        // However, if the content contains newlines, SseEmitter handles it.
                        emitter.send(data);
                    }
                } catch (Exception e) {
                    emitter.completeWithError(e);
                }
            },
            emitter::completeWithError,
            emitter::complete
        );

        return emitter;
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

    private String resolveIngestContent(Post post) {
        if (post == null) {
            return "";
        }
        String content = post.getContent();
        if (content != null && !content.trim().isEmpty()) {
            return content;
        }
        String summary = post.getSummary();
        if (summary != null && !summary.trim().isEmpty()) {
            return summary;
        }
        String title = post.getTitle();
        return title == null ? "" : title;
    }

    @Override
    public void ingestArticle(Long articleId) {
        Post post = postService.getById(articleId);
        if (post == null) return;

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("article_id", articleId.toString());
        requestBody.put("title", post.getTitle());
        requestBody.put("content", resolveIngestContent(post));
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

    @Override
    public Map<String, Object> ingestAllArticles() {
        List<Post> posts = postService.list();
        List<Map<String, Object>> articles = posts.stream()
                .map(post -> {
                    Map<String, Object> article = new HashMap<>();
                    article.put("article_id", post.getId().toString());
                    article.put("title", post.getTitle());
                    article.put("content", resolveIngestContent(post));
                    if (post.getTags() != null) {
                        article.put("tags", Arrays.asList(post.getTags().split(",")));
                    } else {
                        article.put("tags", List.of());
                    }
                    return article;
                })
                .toList();

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("articles", articles);

        try {
            return webClient.post()
                    .uri(aiServiceUrl + "/ingest/bulk")
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
}
