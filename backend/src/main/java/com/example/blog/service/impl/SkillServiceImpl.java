package com.example.blog.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.blog.entity.Skill;
import com.example.blog.mapper.SkillMapper;
import com.example.blog.service.SkillService;
import org.springframework.stereotype.Service;

@Service
public class SkillServiceImpl extends ServiceImpl<SkillMapper, Skill> implements SkillService {
}

