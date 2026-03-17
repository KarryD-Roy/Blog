package com.example.blog.service;

import com.baomidou.mybatisplus.extension.service.IService;
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
}
