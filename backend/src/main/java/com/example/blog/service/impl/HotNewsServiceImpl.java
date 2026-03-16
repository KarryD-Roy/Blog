package com.example.blog.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.blog.entity.HotNews;
import com.example.blog.mapper.HotNewsMapper;
import com.example.blog.service.HotNewsService;
import org.springframework.stereotype.Service;

@Service
public class HotNewsServiceImpl extends ServiceImpl<HotNewsMapper, HotNews> implements HotNewsService {
}
