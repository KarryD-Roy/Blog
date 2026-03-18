package com.example.blog.search;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.blog.entity.Post;

public interface PostSearchService {

    boolean isEnabled();

    IPage<Post> search(long page, long size, String keyword, Long categoryId, String tag);

    Post getById(Long id);

    void index(Post post);

    void delete(Long id);

    void reindexAll();
}
