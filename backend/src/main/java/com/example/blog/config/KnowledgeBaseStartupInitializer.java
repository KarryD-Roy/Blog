package com.example.blog.config;

import com.example.blog.service.KnowledgeBaseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 启动时触发一次知识库全量同步。
 * 仅在向量库为空时执行，用于将数据库中已存在的文章同步到 AI 知识库。
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Order(Integer.MAX_VALUE)
public class KnowledgeBaseStartupInitializer implements CommandLineRunner {

    private final KnowledgeBaseService knowledgeBaseService;

    @Override
    public void run(String... args) {
        try {
            knowledgeBaseService.runStartupFullImport();
            log.info("已提交启动全量知识库导入任务");
        } catch (Exception e) {
            log.error("提交启动全量知识库导入任务失败: {}", e.getMessage(), e);
        }
    }
}
