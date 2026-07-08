package com.example.blog.service;

import com.example.blog.dto.ImportProgressResponse;
import com.example.blog.dto.ImportReportResponse;

/**
 * 知识库批量导入服务接口
 */
public interface KnowledgeBaseService {

    /**
     * 启动异步批量导入任务
     * @return 任务ID
     */
    String startBatchImport();

    /**
     * 查询导入进度
     * @param taskId 任务ID
     * @return 进度信息
     */
    ImportProgressResponse getProgress(String taskId);

    /**
     * 获取导入完成后的状态报告
     * @param taskId 任务ID
     * @return 导入报告
     */
    ImportReportResponse getReport(String taskId);

    /**
     * 取消正在进行的导入任务
     * @param taskId 任务ID
     * @return 是否取消成功
     */
    boolean cancelImport(String taskId);

    /**
     * 验证已导入文章的完整性
     * @param taskId 任务ID
     * @return 验证是否通过
     */
    boolean verifyImport(String taskId);

    /**
     * 启动时执行一次全量知识库同步：仅在知识库为空时触发，
     * 用于将数据库中已存在的文章一次性导入向量知识库。
     */
    void runStartupFullImport();

    /**
     * 将单篇文章增量同步到知识库。
     * 适用于新增/修改文章后的实时同步。
     * @param postId 文章ID
     */
    void syncArticle(Long postId);
}
