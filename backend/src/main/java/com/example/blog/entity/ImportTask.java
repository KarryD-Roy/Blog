package com.example.blog.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 知识库批量导入任务实体
 */
@Data
@TableName("import_tasks")
public class ImportTask {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 任务唯一标识（UUID） */
    private String taskId;

    /** 任务状态：PENDING, RUNNING, COMPLETED, FAILED, CANCELLED */
    private String status;

    /** 待处理文章总数 */
    private Integer totalCount;

    /** 已处理文章数 */
    private Integer processedCount;

    /** 成功导入数 */
    private Integer successCount;

    /** 失败数 */
    private Integer failedCount;

    /** 开始时间 */
    private LocalDateTime startedAt;

    /** 完成时间 */
    private LocalDateTime completedAt;

    /** 错误信息 */
    private String errorMessage;

    /** 当前批次索引 */
    private Integer currentBatch;

    /** 总批次数 */
    private Integer totalBatches;

    /** 知识库导入后文档总数（验证用） */
    private Integer kbDocumentCount;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
