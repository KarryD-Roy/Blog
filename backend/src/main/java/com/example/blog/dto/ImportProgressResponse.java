package com.example.blog.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 导入进度响应 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportProgressResponse {

    private String taskId;
    private String status;
    private Integer totalCount;
    private Integer processedCount;
    private Integer successCount;
    private Integer failedCount;
    private Integer currentBatch;
    private Integer totalBatches;

    /** 进度百分比 (0-100) */
    private Integer progressPercent;

    /** 预估剩余时间(秒) */
    private Long estimatedSecondsRemaining;

    /** 开始时间 */
    private LocalDateTime startedAt;

    /** 错误信息（仅在失败时有值） */
    private String errorMessage;
}
