package com.example.blog.controller;

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

    @GetMapping
    public ApiResponse<List<Skill>> listAll() {
        return ApiResponse.ok(skillService.list());
    }

    @PostMapping
    public ApiResponse<Skill> create(@RequestBody Skill skill) {
        skill.setId(null);
        skillService.save(skill);
        return ApiResponse.ok(skill);
    }

    @PutMapping("/{id}")
    public ApiResponse<Skill> update(@PathVariable Long id, @RequestBody Skill skill) {
        skill.setId(id);
        skillService.updateById(skill);
        return ApiResponse.ok(skill);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        skillService.removeById(id);
        return ApiResponse.ok(null);
    }
}

