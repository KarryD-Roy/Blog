package com.example.blog.search;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.blog.entity.Post;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(value = "search.elasticsearch.enabled", havingValue = "false", matchIfMissing = true)
public class NoopPostSearchService implements PostSearchService {

    @Override
    public boolean isEnabled() {
        return false;
    }

    @Override
    public com.baomidou.mybatisplus.core.metadata.IPage<Post> search(long page, long size, String keyword, Long categoryId, String tag) {
        return Page.of(page, size);
    }

    @Override
    public Post getById(Long id) {
        return null;
    }

    @Override
    public void index(Post post) {
        // no-op
    }

    @Override
    public void delete(Long id) {
        // no-op
    }

    @Override
    public void reindexAll() {
        // no-op
    }
}
