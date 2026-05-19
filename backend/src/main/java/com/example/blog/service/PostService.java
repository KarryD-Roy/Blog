package com.example.blog.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.blog.entity.Post;

public interface PostService extends IService<Post> {
    Post incrementViewCount(Long id);
    void savePostWithSkills(Post post);
    void updatePostWithSkills(Post post);
    java.util.List<Long> getSkillIdsByPostId(Long postId);
}
