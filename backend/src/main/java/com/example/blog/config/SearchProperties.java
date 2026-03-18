package com.example.blog.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "search.elasticsearch")
public class SearchProperties {

    /**
     * Whether Elasticsearch search is enabled.
     */
    private boolean enabled = false;

    /**
     * Comma-separated uris, e.g. http://localhost:9200
     */
    private String uris;

    private String username;

    private String password;

    /**
     * Index name used to store blog posts.
     */
    private String index = "blog_posts";
}
