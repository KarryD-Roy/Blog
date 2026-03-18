package com.example.blog.config;

import com.example.blog.search.PostSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class DataInitializer {

    private final PostSearchService postSearchService;

    @Bean
    public CommandLineRunner initData() {
        return args -> {
            if (postSearchService.isEnabled()) {
                log.info("Starting to sync blog posts from database to Elasticsearch...");
                try {
                    postSearchService.reindexAll();
                    log.info("Successfully synced blog posts to Elasticsearch.");
                } catch (Exception e) {
                    log.error("Failed to sync blog posts to Elasticsearch: {}", e.getMessage(), e);
                }
            } else {
                log.info("Elasticsearch is disabled, skipping data sync.");
            }
        };
    }
}
