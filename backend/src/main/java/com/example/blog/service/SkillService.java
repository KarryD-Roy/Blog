package com.example.blog.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.blog.dto.SkillCoordUpdate;
import com.example.blog.dto.SkillGraphResponse;
import com.example.blog.entity.Post;
import com.example.blog.entity.Skill;

import java.util.List;

public interface SkillService extends IService<Skill> {
    /**
     * 记录技能被访问，用于「访问过的技能」列表。
     */
    void recordVisitedSkill(Long skillId);

    /**
     * 获取最近访问过的技能（按访问时间倒序）。
     */
    List<Skill> listVisitedSkills();

    /**
     * 获取技能树节点与连线。
     */
    SkillGraphResponse getSkillGraph();

    /**
     * 更新技能节点坐标（带乐观锁）。
     */
    boolean updateSkillCoords(List<SkillCoordUpdate> updates);

    /**
     * 获取某个技能关联的文章分页。
     */
    IPage<Post> listPostsBySkill(Long skillId, long page, long size);
}
