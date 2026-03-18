package com.example.blog.search;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.blog.config.SearchProperties;
import com.example.blog.entity.Post;
import com.example.blog.mapper.PostMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders.multiMatch;
import static co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders.term;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(value = "search.elasticsearch.enabled", havingValue = "true")
public class PostSearchServiceImpl implements PostSearchService {

    private final SearchProperties searchProperties;
    private final ElasticsearchOperations operations;
    private final PostSearchRepository repository;
    private final PostMapper postMapper;

    @Override
    public boolean isEnabled() {
        return searchProperties.isEnabled();
    }

    @Override
    public com.baomidou.mybatisplus.core.metadata.IPage<Post> search(long page, long size, String keyword, Long categoryId, String tag) {
        if (!isEnabled()) {
            return Page.of(page, size);
        }

        var builder = NativeQuery.builder();
        builder.withPageable(PageRequest.of(Math.toIntExact(page - 1), Math.toIntExact(size)));
        builder.withQuery(q -> q.bool(b -> {
            if (StringUtils.hasText(keyword)) {
                b.must(multiMatch(mm -> mm
                        .fields("title", "summary", "content", "tags_text")
                        .query(keyword)));
            }
            if (categoryId != null) {
                b.filter(term(t -> t.field("categoryId").value(categoryId)));
            }
            if (StringUtils.hasText(tag)) {
                b.filter(term(t -> t.field("tags").value(tag.trim())));
            }
            return b;
        }));
        builder.withSort(Sort.by(Sort.Order.desc("pinned"), Sort.Order.desc("createdAt")));

        SearchHits<PostDocument> hits = operations.search(builder.build(), PostDocument.class);
        List<Post> records = hits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .map(this::toPost)
                .collect(Collectors.toList());

        Page<Post> result = Page.of(page, size);
        result.setRecords(records);
        result.setTotal(hits.getTotalHits());
        return result;
    }

    @Override
    public void index(Post post) {
        if (!isEnabled() || post == null || post.getId() == null) {
            return;
        }
        repository.save(toDocument(post));
    }

    @Override
    public void delete(Long id) {
        if (!isEnabled() || id == null) {
            return;
        }
        repository.deleteById(id);
    }

    @Override
    public Post getById(Long id) {
        if (!isEnabled()) {
            return null;
        }
        return repository.findById(id).map(this::toPost).orElse(null);
    }

    @Override
    public void reindexAll() {
        if (!isEnabled()) {
            return;
        }
        NativeQuery deleteAllQuery = NativeQuery.builder()
                .withQuery(q -> q.matchAll(m -> m))
                .build();
        operations.delete(deleteAllQuery, PostDocument.class, IndexCoordinates.of(searchProperties.getIndex()));
        List<Post> posts = postMapper.selectList(null);
        if (posts == null || posts.isEmpty()) {
            return;
        }
        List<PostDocument> docs = posts.stream().map(this::toDocument).toList();
        repository.saveAll(docs);
    }

    private PostDocument toDocument(Post post) {
        PostDocument doc = new PostDocument();
        doc.setId(post.getId());
        doc.setTitle(post.getTitle());
        doc.setSummary(post.getSummary());
        doc.setContent(post.getContent());
        doc.setCategoryId(post.getCategoryId());
        doc.setPinned(Boolean.TRUE.equals(post.getPinned()));
        doc.setViewCount(post.getViewCount() == null ? 0L : post.getViewCount().longValue());
        doc.setCreatedAt(post.getCreatedAt());
        doc.setUpdatedAt(post.getUpdatedAt());
        doc.setTags(splitTags(post.getTags()));
        return doc;
    }

    private Post toPost(PostDocument doc) {
        Post post = new Post();
        post.setId(doc.getId());
        post.setTitle(doc.getTitle());
        post.setSummary(doc.getSummary());
        post.setContent(doc.getContent());
        post.setCategoryId(doc.getCategoryId());
        post.setPinned(doc.getPinned());
        post.setViewCount(doc.getViewCount() == null ? 0 : doc.getViewCount().intValue());
        post.setCreatedAt(doc.getCreatedAt());
        post.setUpdatedAt(doc.getUpdatedAt());
        post.setTags(doc.getTags() == null ? null : String.join(",", doc.getTags()));
        return post;
    }

    private List<String> splitTags(String tags) {
        if (!StringUtils.hasText(tags)) {
            return Collections.emptyList();
        }
        return Arrays.stream(tags.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toList();
    }
}
