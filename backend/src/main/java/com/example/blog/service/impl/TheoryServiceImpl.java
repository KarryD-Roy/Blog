package com.example.blog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.blog.entity.Theory;
import com.example.blog.mapper.TheoryMapper;
import com.example.blog.service.TheoryService;
import org.springframework.stereotype.Service;

@Service
public class TheoryServiceImpl extends ServiceImpl<TheoryMapper, Theory> implements TheoryService {
    @Override
    public Theory getBySkillId(Long skillId) {
        return getOne(new LambdaQueryWrapper<Theory>().eq(Theory::getSkillId, skillId));
    }
}
