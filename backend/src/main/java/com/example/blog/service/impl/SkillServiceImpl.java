package com.example.blog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.blog.dto.SkillCoordUpdate;
import com.example.blog.dto.SkillGraphResponse;
import com.example.blog.dto.SkillLink;
import com.example.blog.entity.Post;
import com.example.blog.entity.PostSkillRelation;
import com.example.blog.entity.Skill;
import com.example.blog.mapper.PostMapper;
import com.example.blog.mapper.PostSkillRelationMapper;
import com.example.blog.mapper.SkillMapper;
import com.example.blog.service.AiService;
import com.example.blog.service.SkillService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SkillServiceImpl extends ServiceImpl<SkillMapper, Skill> implements SkillService {

    private static final String VISITED_KEY = "skill:visited";
    private static final int VISITED_MAX = 20;
    private static final Duration VISITED_TTL = Duration.ofDays(7);

    private final StringRedisTemplate stringRedisTemplate;
    private final PostSkillRelationMapper postSkillRelationMapper;
    private final PostMapper postMapper;
    private final AiService aiService;

    @Override
    public void recordVisitedSkill(Long skillId) {
        String idStr = String.valueOf(skillId);
        stringRedisTemplate.opsForList().remove(VISITED_KEY, 0, idStr);
        stringRedisTemplate.opsForList().leftPush(VISITED_KEY, idStr);
        stringRedisTemplate.opsForList().trim(VISITED_KEY, 0, VISITED_MAX - 1);
        stringRedisTemplate.expire(VISITED_KEY, VISITED_TTL);
    }

    @Override
    public List<Skill> listVisitedSkills() {
        List<String> ids = stringRedisTemplate.opsForList().range(VISITED_KEY, 0, VISITED_MAX - 1);
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        List<Long> longIds = ids.stream().map(Long::valueOf).toList();
        List<Skill> skills = listByIds(longIds);
        if (skills.isEmpty()) {
            return List.of();
        }
        Map<Long, Skill> byId = skills.stream()
                .collect(Collectors.toMap(Skill::getId, Function.identity()));
        List<Skill> ordered = new ArrayList<>();
        for (String idStr : ids) {
            Skill skill = byId.get(Long.valueOf(idStr));
            if (skill != null) {
                ordered.add(skill);
            }
        }
        return ordered;
    }

    @Override
    public SkillGraphResponse getSkillGraph() {
        List<Skill> skills = list();
        List<SkillLink> links = skills.stream()
                .filter(skill -> skill.getParentId() != null)
                .map(skill -> new SkillLink(skill.getParentId(), skill.getId()))
                .toList();
        return new SkillGraphResponse(skills, links);
    }

    @Override
    public boolean updateSkillCoords(List<SkillCoordUpdate> updates) {
        if (updates == null || updates.isEmpty()) {
            return true;
        }
        for (SkillCoordUpdate update : updates) {
            if (update.getId() == null || update.getVersion() == null) {
                return false;
            }
            Skill toUpdate = new Skill();
            toUpdate.setId(update.getId());
            toUpdate.setXAxis(update.getXAxis());
            toUpdate.setYAxis(update.getYAxis());
            toUpdate.setVersion(update.getVersion());
            boolean updated = updateById(toUpdate);
            if (!updated) {
                return false;
            }
        }
        return true;
    }

    @Override
    public IPage<Post> listPostsBySkill(Long skillId, long page, long size) {
        Skill skill = getById(skillId);
        if (skill == null) return Page.of(page, size);

        // 1. First path: Database relations (Precise)
        LambdaQueryWrapper<PostSkillRelation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PostSkillRelation::getSkillId, skillId);
        List<PostSkillRelation> relations = postSkillRelationMapper.selectList(wrapper);

        List<Long> dbPostIds = relations.stream().map(PostSkillRelation::getPostId).toList();

        // 2. Second path: AI Recommendations (Semantic)
        // If we don't have enough DB relations, or always want to mix, we call AI
        Map<String, Object> aiResult = aiService.getRecommendations(skill.getTitle());
        List<Map<String, Object>> relatedChunks = (List<Map<String, Object>>) aiResult.getOrDefault("related_chunks", List.of());

        List<Long> aiPostIds = relatedChunks.stream()
                .map(chunk -> {
                    Map<String, Object> metadata = (Map<String, Object>) chunk.get("metadata");
                    Object id = metadata.get("article_id");
                    return id != null ? Long.valueOf(id.toString()) : null;
                })
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();

        // Combine IDs, keeping DB relations first as they are more "official"
        List<Long> combinedIds = new ArrayList<>(dbPostIds);
        for (Long aiId : aiPostIds) {
            if (!combinedIds.contains(aiId)) {
                combinedIds.add(aiId);
            }
        }

        if (combinedIds.isEmpty()) {
            return Page.of(page, size);
        }

        // Pagination for combined results
        int start = (int) ((page - 1) * size);
        int end = Math.min(start + (int) size, combinedIds.size());

        if (start >= combinedIds.size()) {
            return Page.of(page, size, combinedIds.size());
        }

        List<Long> pagedIds = combinedIds.subList(start, end);
        List<Post> posts = postMapper.selectBatchIds(pagedIds);

        // Ensure order is preserved
        Map<Long, Post> postMap = posts.stream().collect(Collectors.toMap(Post::getId, Function.identity()));
        List<Post> orderedPosts = pagedIds.stream().map(postMap::get).filter(java.util.Objects::nonNull).toList();

        Page<Post> result = Page.of(page, size, combinedIds.size());
        result.setRecords(orderedPosts);
        return result;
    }
}
