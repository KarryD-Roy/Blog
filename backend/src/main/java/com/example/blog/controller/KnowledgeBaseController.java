package com.example.blog.controller;

import com.example.blog.dto.ImportProgressResponse;
import com.example.blog.dto.ImportReportResponse;
import com.example.blog.service.KnowledgeBaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 知识库批量导入控制器
 * 提供导入启动、进度查询、状态报告、取消、验证等 REST 接口
 */
@RestController
@RequestMapping("/api/knowledge-base")
@RequiredArgsConstructor
public class KnowledgeBaseController {

    private final KnowledgeBaseService knowledgeBaseService;

    /**
     * 启动批量导入任务
     * 扫描数据库中全部已发布文章，分批并发导入到向量知识库
     */
    @PostMapping("/import")
    public ApiResponse<String> startImport() {
        try {
            String taskId = knowledgeBaseService.startBatchImport();
            return ApiResponse.ok(taskId);
        } catch (Exception e) {
            return ApiResponse.error("启动导入失败: " + e.getMessage());
        }
    }

    /**
     * 查询导入进度
     * 返回已处理/成功/失败文章数、批次进度、预估剩余时间
     */
    @GetMapping("/import/status/{taskId}")
    public ApiResponse<ImportProgressResponse> getProgress(@PathVariable String taskId) {
        try {
            ImportProgressResponse progress = knowledgeBaseService.getProgress(taskId);
            return ApiResponse.ok(progress);
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    /**
     * 获取导入完成后的状态报告
     * 包含统计信息、知识库验证结果
     */
    @GetMapping("/import/report/{taskId}")
    public ApiResponse<ImportReportResponse> getReport(@PathVariable String taskId) {
        try {
            ImportReportResponse report = knowledgeBaseService.getReport(taskId);
            return ApiResponse.ok(report);
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    /**
     * 取消正在运行的导入任务
     */
    @PostMapping("/import/cancel/{taskId}")
    public ApiResponse<Boolean> cancelImport(@PathVariable String taskId) {
        boolean cancelled = knowledgeBaseService.cancelImport(taskId);
        return cancelled
                ? ApiResponse.ok(true)
                : ApiResponse.error("取消失败：任务不存在或已结束");
    }

    /**
     * 手动触发知识库文章完整性验证
     */
    @PostMapping("/import/verify/{taskId}")
    public ApiResponse<Boolean> verifyImport(@PathVariable String taskId) {
        boolean verified = knowledgeBaseService.verifyImport(taskId);
        return verified
                ? ApiResponse.ok(true)
                : ApiResponse.error("验证失败，请检查 AI 服务状态");
    }
}
