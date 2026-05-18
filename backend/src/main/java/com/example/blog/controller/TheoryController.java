package com.example.blog.controller;

import com.example.blog.entity.Theory;
import com.example.blog.service.TheoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/theories")
@RequiredArgsConstructor
public class TheoryController {

    private final TheoryService theoryService;

    @GetMapping("/skill/{skillId}")
    public ApiResponse<Theory> getBySkillId(@PathVariable Long skillId) {
        Theory theory = theoryService.getBySkillId(skillId);
        return ApiResponse.ok(theory);
    }

    @PostMapping
    public ApiResponse<Theory> saveOrUpdate(@RequestBody Theory theory) {
        Theory existing = theoryService.getBySkillId(theory.getSkillId());
        if (existing != null) {
            theory.setId(existing.getId());
            theory.setUpdatedAt(LocalDateTime.now());
            theoryService.updateById(theory);
        } else {
            theory.setCreatedAt(LocalDateTime.now());
            theory.setUpdatedAt(LocalDateTime.now());
            theoryService.save(theory);
        }
        return ApiResponse.ok(theory);
    }
}
