package com.example.blog.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.blog.entity.Theory;

public interface TheoryService extends IService<Theory> {
    Theory getBySkillId(Long skillId);
}
