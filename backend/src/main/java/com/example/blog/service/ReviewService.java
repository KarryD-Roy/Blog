package com.example.blog.service;

import com.example.blog.entity.Post;
import com.example.blog.entity.Skill;

import java.util.List;

public interface ReviewService {
    void reviewPost(Long postId, String status, String reason, Long reviewerId);
    void reviewSkill(Long skillId, String status, String reason, Long reviewerId);
    List<Post> getPendingPosts();
    List<Skill> getPendingSkills();
}
