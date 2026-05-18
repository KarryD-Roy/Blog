package com.example.blog.dto;

import com.example.blog.entity.Skill;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SkillGraphResponse {

    private List<Skill> nodes;

    private List<SkillLink> links;
}
