package com.example.blog.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.blog.entity.Skill;
import com.example.blog.mapper.SkillMapper;
import com.example.blog.service.SkillService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
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
}
