package com.example.blog.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.blog.entity.Post;
import com.example.blog.entity.PostSkillRelation;
import com.example.blog.mapper.PostMapper;
import com.example.blog.mapper.PostSkillRelationMapper;
import com.example.blog.service.PostService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class PostServiceImpl extends ServiceImpl<PostMapper, Post> implements PostService {

    private final PostSkillRelationMapper postSkillRelationMapper;

    public PostServiceImpl(PostSkillRelationMapper postSkillRelationMapper) {
        this.postSkillRelationMapper = postSkillRelationMapper;
    }

    // 文章浏览量自增，使用 SQL 方式避免并发问题
    @Override
    public Post incrementViewCount(Long id) {
        LambdaUpdateWrapper<Post> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Post::getId, id).setSql("view_count = COALESCE(view_count, 0) + 1");
        int updated = this.baseMapper.update(null, updateWrapper);
        if (updated > 0) {
            return this.getById(id);
        }
        return null;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void savePostWithSkills(Post post) {
        this.save(post);
        saveSkillRelations(post.getId(), post.getSkillIds());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePostWithSkills(Post post) {
        this.updateById(post);
        postSkillRelationMapper.delete(
            new LambdaQueryWrapper<PostSkillRelation>().eq(PostSkillRelation::getPostId, post.getId())
        );
        saveSkillRelations(post.getId(), post.getSkillIds());
    }

    @Override
    public List<Long> getSkillIdsByPostId(Long postId) {
        List<PostSkillRelation> relations = postSkillRelationMapper.selectList(
            new LambdaQueryWrapper<PostSkillRelation>().eq(PostSkillRelation::getPostId, postId)
        );
        if (relations == null) return List.of();
        return relations.stream().map(PostSkillRelation::getSkillId).collect(Collectors.toList());
    }

    private void saveSkillRelations(Long postId, List<Long> skillIds) {
        if (skillIds != null && !skillIds.isEmpty()) {
            for (Long skillId : skillIds) {
                PostSkillRelation relation = new PostSkillRelation();
                relation.setPostId(postId);
                relation.setSkillId(skillId);
                postSkillRelationMapper.insert(relation);
            }
        }
    }
}
