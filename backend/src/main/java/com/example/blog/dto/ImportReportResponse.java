package com.example.blog.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 导入完成后的状态报告 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportReportResponse {

    /** 任务ID */
    private String taskId;

    /** 最终状态 */
    private String status;

    /** 统计信息 */
    private Integer totalCount;
    private Integer successCount;
    private Integer failedCount;

    /** 耗时(秒) */
    private Long durationSeconds;

    /** 开始时间 */
    private LocalDateTime startedAt;

    /** 完成时间 */
    private LocalDateTime completedAt;

    /** 知识库验证结果 */
    private VerificationResult verification;

    /** 失败的文章详情列表 */
    private List<FailedArticleDetail> failedDetails;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VerificationResult {
        /** 知识库中当前文档总数 */
        private Integer kbDocumentCount;
        /** 验证是否通过 */
        private Boolean passed;
        /** 验证信息 */
        private String message;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FailedArticleDetail {
        /** 文章ID */
        private Long articleId;
        /** 文章标题 */
        private String title;
        /** 失败原因 */
        private String reason;
    }
}
