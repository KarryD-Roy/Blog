package com.example.blog.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.blog.dto.SkillCoordUpdate;
import com.example.blog.dto.SkillGraphResponse;
import com.example.blog.entity.Post;
import com.example.blog.entity.Skill;
import com.example.blog.service.SkillService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/skills")
@RequiredArgsConstructor
public class SkillController {

    private final SkillService skillService;

    /**
     * 返回全部技能（旧接口，保留兼容）
     */
    @GetMapping
    public ApiResponse<List<Skill>> listAll() {
        return ApiResponse.ok(skillService.list());
    }

    /**
     * 技能树数据
     */
    @GetMapping("/graph")
    public ApiResponse<SkillGraphResponse> graph() {
        return ApiResponse.ok(skillService.getSkillGraph());
    }

    /**
     * 技能分页接口，技能树页面使用
     */
    @GetMapping("/page")
    public ApiResponse<IPage<Skill>> page(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "6") long size) {
        LambdaQueryWrapper<Skill> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(Skill::getPinned).orderByAsc(Skill::getId);
        IPage<Skill> result = skillService.page(Page.of(page, size), wrapper);
        return ApiResponse.ok(result);
    }

    /**
     * 获取技能关联文章列表
     */
    @GetMapping("/{id}/posts")
    public ApiResponse<IPage<Post>> postsBySkill(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "6") long size) {
        return ApiResponse.ok(skillService.listPostsBySkill(id, page, size));
    }

    /**
     * 批量保存技能坐标
     */
    @PutMapping("/coords")
    public ApiResponse<Void> updateCoords(@RequestBody List<SkillCoordUpdate> updates) {
        boolean updated = skillService.updateSkillCoords(updates);
        if (!updated) {
            return ApiResponse.error("坐标保存失败，可能已被其他用户更新");
        }
        return ApiResponse.ok(null);
    }

    /**
     * 最近访问过的技能（按访问时间倒序，最多 20 条）
     */
    @GetMapping("/visited")
    public ApiResponse<List<Skill>> visited() {
        return ApiResponse.ok(skillService.listVisitedSkills());
    }

    /**
     * 记录技能访问行为。
     */
    @PostMapping("/{id}/visit")
    public ApiResponse<Void> visit(@PathVariable Long id) {
        Skill skill = skillService.getById(id);
        if (skill == null) {
            return ApiResponse.error("技能不存在");
        }
        skillService.recordVisitedSkill(id);
        return ApiResponse.ok(null);
    }

    @PostMapping
    public ApiResponse<Skill> create(@RequestBody Skill skill) {
        skill.setId(null);
        skillService.save(skill);

        // Asynchronously re-ingest articles or trigger AI to recognize the new node
        // Actually, ingestArticle usually pushes article TO AI.
        // For semantic linking, AI service just needs to know about articles.
        // When we call getRecommendations later, it uses the existing vector DB.

        return ApiResponse.ok(skill);
    }

    @PutMapping("/{id}")
    public ApiResponse<Skill> update(@PathVariable Long id, @RequestBody Skill skill) {
        skill.setId(id);
        skillService.updateById(skill);
        return ApiResponse.ok(skill);
    }

    /**
     * 单独更新技能置顶状态
     */
    @PutMapping("/{id}/pin")
    public ApiResponse<Skill> updatePin(@PathVariable Long id, @RequestParam boolean pinned) {
        Skill skill = skillService.getById(id);
        if (skill == null) {
            return ApiResponse.error("技能不存在");
        }
        skill.setPinned(pinned);
        skillService.updateById(skill);
        return ApiResponse.ok(skill);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        skillService.removeById(id);
        return ApiResponse.ok(null);
    }
}
