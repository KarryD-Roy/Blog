package com.example.blog.service.impl;

import com.example.blog.dto.ImportProgressResponse;
import com.example.blog.dto.ImportReportResponse;
import com.example.blog.entity.ImportTask;
import com.example.blog.entity.Post;
import com.example.blog.mapper.ImportTaskMapper;
import com.example.blog.service.KnowledgeBaseService;
import com.example.blog.service.PostService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.http.MediaType;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * 知识库批量导入服务实现
 * 支持：分批并发导入、进度追踪、错误重试、完整性验证
 */
@Service
public class KnowledgeBaseServiceImpl implements KnowledgeBaseService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeBaseServiceImpl.class);

    /** 每批导入的文章数量 */
    private static final int BATCH_SIZE = 20;

    /** 并发批次数 */
    private static final int CONCURRENT_BATCHES = 3;

    /** 单批次最大重试次数 */
    private static final int MAX_RETRY = 3;

    /** 重试间隔基数(毫秒) */
    private static final long RETRY_BASE_DELAY_MS = 1000;

    private final PostService postService;
    private final ImportTaskMapper importTaskMapper;
    private final WebClient webClient;

    /** 运行中的任务状态缓存（taskId -> ImportTask） */
    private final Map<String, ImportTask> runningTasks = new ConcurrentHashMap<>();

    /** 取消标记 */
    private final Set<String> cancelledTasks = ConcurrentHashMap.newKeySet();

    @Value("${ai.service.url}")
    private String aiServiceUrl;

    public KnowledgeBaseServiceImpl(PostService postService,
                                     ImportTaskMapper importTaskMapper,
                                     WebClient.Builder webClientBuilder) {
        this.postService = postService;
        this.importTaskMapper = importTaskMapper;
        this.webClient = webClientBuilder.build();
    }

    @Override
    @Transactional
    public String startBatchImport() {
        // 检查是否有正在运行的任务
        for (ImportTask task : runningTasks.values()) {
            if ("RUNNING".equals(task.getStatus())) {
                throw new RuntimeException("已有导入任务正在运行中: " + task.getTaskId());
            }
        }

        String taskId = UUID.randomUUID().toString();
        List<Post> allPosts = postService.list();
        int totalCount = allPosts.size();

        if (totalCount == 0) {
            throw new RuntimeException("没有可导入的文章");
        }

        int totalBatches = (int) Math.ceil((double) totalCount / BATCH_SIZE);

        ImportTask task = new ImportTask();
        task.setTaskId(taskId);
        task.setStatus("PENDING");
        task.setTotalCount(totalCount);
        task.setProcessedCount(0);
        task.setSuccessCount(0);
        task.setFailedCount(0);
        task.setTotalBatches(totalBatches);
        task.setCurrentBatch(0);
        task.setCreatedAt(LocalDateTime.now());
        importTaskMapper.insert(task);

        runningTasks.put(taskId, task);

        // 异步执行导入
        executeBatchImport(taskId);

        log.info("批量导入任务已创建: taskId={}, total={}, batches={}", taskId, totalCount, totalBatches);
        return taskId;
    }

    @Async("importTaskExecutor")
    public void executeBatchImport(String taskId) {
        ImportTask task = runningTasks.get(taskId);
        if (task == null) return;

        task.setStatus("RUNNING");
        task.setStartedAt(LocalDateTime.now());
        updateTaskInDb(task);

        List<Post> allPosts = postService.list();
        int totalBatches = task.getTotalBatches();

        // 按批次分组
        List<List<Post>> batches = new ArrayList<>();
        for (int i = 0; i < allPosts.size(); i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, allPosts.size());
            batches.add(allPosts.subList(i, end));
        }

        AtomicInteger successTotal = new AtomicInteger(0);
        AtomicInteger failedTotal = new AtomicInteger(0);
        AtomicInteger batchIndex = new AtomicInteger(0);

        // 收集失败详情
        List<ImportReportResponse.FailedArticleDetail> failedDetails =
                Collections.synchronizedList(new ArrayList<>());

        // 分批处理
        for (int i = 0; i < batches.size(); i += CONCURRENT_BATCHES) {
            if (cancelledTasks.contains(taskId)) {
                task.setStatus("CANCELLED");
                task.setCompletedAt(LocalDateTime.now());
                updateTaskInDb(task);
                log.warn("任务已取消: taskId={}", taskId);
                return;
            }

            int endIdx = Math.min(i + CONCURRENT_BATCHES, batches.size());
            List<List<Post>> concurrentBatchGroup = batches.subList(i, endIdx);

            for (List<Post> batch : concurrentBatchGroup) {
                if (cancelledTasks.contains(taskId)) break;

                int retryCount = 0;
                boolean batchSuccess = false;

                while (!batchSuccess && retryCount < MAX_RETRY && !cancelledTasks.contains(taskId)) {
                    try {
                        boolean result = importBatch(batch, taskId);
                        if (result) {
                            batchSuccess = true;
                            successTotal.addAndGet(batch.size());
                        } else {
                            retryCount++;
                            if (retryCount < MAX_RETRY) {
                                long delay = RETRY_BASE_DELAY_MS * (1L << (retryCount - 1));
                                log.warn("批次导入失败，{}ms后重试 ({}/{}): taskId={}",
                                        delay, retryCount, MAX_RETRY, taskId);
                                Thread.sleep(delay);
                            }
                        }
                    } catch (Exception e) {
                        retryCount++;
                        log.error("批次导入异常: taskId={}, error={}", taskId, e.getMessage());
                        if (retryCount >= MAX_RETRY) {
                            failedTotal.addAndGet(batch.size());
                            for (Post post : batch) {
                                failedDetails.add(ImportReportResponse.FailedArticleDetail.builder()
                                        .articleId(post.getId())
                                        .title(post.getTitle())
                                        .reason(e.getMessage())
                                        .build());
                            }
                        } else {
                            long delay = RETRY_BASE_DELAY_MS * (1L << (retryCount - 1));
                            try { Thread.sleep(delay); } catch (InterruptedException ignored) {}
                        }
                    }
                }

                if (!batchSuccess && !cancelledTasks.contains(taskId)) {
                    failedTotal.addAndGet(batch.size());
                }
            }

            // 更新进度
            int processed = Math.min((i + CONCURRENT_BATCHES) * BATCH_SIZE, allPosts.size());
            task.setProcessedCount(processed);
            task.setSuccessCount(successTotal.get());
            task.setFailedCount(failedTotal.get());
            task.setCurrentBatch(Math.min((i + CONCURRENT_BATCHES), totalBatches));

            if (processed % (BATCH_SIZE * 3) == 0 || processed >= allPosts.size()) {
                updateTaskInDb(task);
                log.info("导入进度: taskId={}, processed={}/{}, success={}, failed={}",
                        taskId, processed, allPosts.size(), successTotal.get(), failedTotal.get());
            }
        }

        // 导入完成
        task.setStatus(cancelledTasks.contains(taskId) ? "CANCELLED" : "COMPLETED");
        task.setCompletedAt(LocalDateTime.now());
        task.setProcessedCount(allPosts.size());
        task.setSuccessCount(successTotal.get());
        task.setFailedCount(failedTotal.get());
        updateTaskInDb(task);

        // 执行完整性验证
        try {
            verifyImport(taskId);
        } catch (Exception e) {
            log.error("导入后验证失败: taskId={}, error={}", taskId, e.getMessage());
        }

        log.info("批量导入完成: taskId={}, status={}, success={}, failed={}",
                taskId, task.getStatus(), successTotal.get(), failedTotal.get());
    }

    /**
     * 导入单个批次
     */
    private boolean importBatch(List<Post> batch, String taskId) {
        List<Map<String, Object>> articles = batch.stream()
                .map(post -> {
                    Map<String, Object> article = new HashMap<>();
                    article.put("article_id", post.getId().toString());
                    article.put("title", post.getTitle());
                    article.put("content", resolveContent(post));
                    article.put("tags", post.getTags() != null
                            ? Arrays.asList(post.getTags().split(","))
                            : List.of());
                    return article;
                })
                .collect(Collectors.toList());

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("articles", articles);

        try {
            Map<String, Object> response = webClient.post()
                    .uri(aiServiceUrl + "/ingest/bulk")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .retryWhen(Retry.backoff(2, Duration.ofSeconds(1))
                            .maxBackoff(Duration.ofSeconds(5)))
                    .block(Duration.ofSeconds(120));

            if (response != null && "success".equals(response.get("status"))) {
                log.debug("批次导入成功: taskId={}, count={}", taskId, batch.size());
                return true;
            } else {
                log.warn("批次导入返回异常: taskId={}, response={}", taskId, response);
                return false;
            }
        } catch (Exception e) {
            log.error("批次导入请求失败: taskId={}, size={}, error={}",
                    taskId, batch.size(), e.getMessage());
            return false;
        }
    }

    @Override
    public ImportProgressResponse getProgress(String taskId) {
        ImportTask task = runningTasks.get(taskId);
        if (task == null) {
            task = importTaskMapper.selectOne(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ImportTask>()
                            .eq(ImportTask::getTaskId, taskId));
        }
        if (task == null) {
            throw new RuntimeException("任务不存在: " + taskId);
        }

        int progressPercent = task.getTotalCount() != null && task.getTotalCount() > 0
                ? (int) ((task.getProcessedCount() * 100.0) / task.getTotalCount())
                : 0;

        Long estimatedRemaining = null;
        if ("RUNNING".equals(task.getStatus()) && task.getStartedAt() != null
                && task.getProcessedCount() != null && task.getProcessedCount() > 0) {
            long elapsed = Duration.between(task.getStartedAt(), LocalDateTime.now()).getSeconds();
            double rate = task.getProcessedCount() / (double) elapsed;
            int remaining = task.getTotalCount() - task.getProcessedCount();
            estimatedRemaining = rate > 0 ? (long) (remaining / rate) : null;
        }

        return ImportProgressResponse.builder()
                .taskId(task.getTaskId())
                .status(task.getStatus())
                .totalCount(task.getTotalCount())
                .processedCount(task.getProcessedCount())
                .successCount(task.getSuccessCount())
                .failedCount(task.getFailedCount())
                .currentBatch(task.getCurrentBatch())
                .totalBatches(task.getTotalBatches())
                .progressPercent(progressPercent)
                .estimatedSecondsRemaining(estimatedRemaining)
                .startedAt(task.getStartedAt())
                .errorMessage(task.getErrorMessage())
                .build();
    }

    @Override
    public ImportReportResponse getReport(String taskId) {
        ImportTask task = runningTasks.get(taskId);
        if (task == null) {
            task = importTaskMapper.selectOne(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ImportTask>()
                            .eq(ImportTask::getTaskId, taskId));
        }
        if (task == null) {
            throw new RuntimeException("任务不存在: " + taskId);
        }

        long durationSeconds = 0;
        if (task.getStartedAt() != null && task.getCompletedAt() != null) {
            durationSeconds = Duration.between(task.getStartedAt(), task.getCompletedAt()).getSeconds();
        }

        ImportReportResponse.VerificationResult verification =
                ImportReportResponse.VerificationResult.builder()
                        .kbDocumentCount(task.getKbDocumentCount())
                        .passed(task.getKbDocumentCount() != null
                                && task.getKbDocumentCount() >= task.getSuccessCount())
                        .message(task.getKbDocumentCount() != null
                                ? String.format("知识库文档数: %d, 成功导入文章数: %d",
                                    task.getKbDocumentCount(), task.getSuccessCount())
                                : "未执行验证")
                        .build();

        return ImportReportResponse.builder()
                .taskId(task.getTaskId())
                .status(task.getStatus())
                .totalCount(task.getTotalCount())
                .successCount(task.getSuccessCount())
                .failedCount(task.getFailedCount())
                .durationSeconds(durationSeconds)
                .startedAt(task.getStartedAt())
                .completedAt(task.getCompletedAt())
                .verification(verification)
                .failedDetails(List.of()) // 简化处理，失败详情可通过日志查看
                .build();
    }

    @Override
    public boolean cancelImport(String taskId) {
        cancelledTasks.add(taskId);
        ImportTask task = runningTasks.get(taskId);
        if (task != null && "RUNNING".equals(task.getStatus())) {
            task.setStatus("CANCELLED");
            task.setCompletedAt(LocalDateTime.now());
            updateTaskInDb(task);
            log.info("任务取消中: taskId={}", taskId);
            return true;
        }
        return false;
    }

    @Override
    public boolean verifyImport(String taskId) {
        ImportTask task = runningTasks.get(taskId);
        if (task == null) {
            task = importTaskMapper.selectOne(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ImportTask>()
                            .eq(ImportTask::getTaskId, taskId));
        }
        if (task == null) return false;

        try {
            Map<String, Object> status = webClient.get()
                    .uri(aiServiceUrl + "/rag/status")
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block(Duration.ofSeconds(10));

            if (status != null && status.get("document_count") instanceof Integer count) {
                task.setKbDocumentCount(count);
                updateTaskInDb(task);
                log.info("验证完成: taskId={}, kbDocs={}, successCount={}",
                        taskId, count, task.getSuccessCount());
                return true;
            }
            return false;
        } catch (Exception e) {
            log.error("验证请求失败: taskId={}, error={}", taskId, e.getMessage());
            return false;
        }
    }

    private void updateTaskInDb(ImportTask task) {
        try {
            importTaskMapper.updateById(task);
        } catch (Exception e) {
            log.error("更新任务状态失败: taskId={}, error={}", task.getTaskId(), e.getMessage());
        }
    }

    private String resolveContent(Post post) {
        if (post == null) return "";
        String content = post.getContent();
        if (content != null && !content.trim().isEmpty()) return content;
        String summary = post.getSummary();
        if (summary != null && !summary.trim().isEmpty()) return summary;
        String title = post.getTitle();
        return title == null ? "" : title;
    }

    @Override
    @Async("importTaskExecutor")
    public void runStartupFullImport() {
        List<Post> allPosts = postService.list();
        if (allPosts.isEmpty()) {
            log.info("数据库中暂无文章，跳过启动全量知识库导入");
            return;
        }

        log.info("准备执行启动全量知识库导入，等待 AI 服务就绪...");
        boolean ready = waitForAiServiceReady(30, 2000L);
        if (!ready) {
            log.error("AI 服务长时间未就绪，跳过启动全量知识库导入");
            return;
        }

        Integer docCount = fetchKnowledgeBaseDocumentCount();
        if (docCount != null && docCount > 0) {
            log.info("知识库已存在 {} 条文档，跳过启动全量导入（避免重复数据）", docCount);
            return;
        }

        log.info("开始全量导入，共 {} 篇文章", allPosts.size());
        int total = allPosts.size();
        int success = 0;
        int failed = 0;
        int batchNum = 0;

        for (int i = 0; i < allPosts.size(); i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, allPosts.size());
            List<Post> batch = allPosts.subList(i, end);
            batchNum++;

            List<Map<String, Object>> articles = batch.stream()
                    .map(post -> {
                        Map<String, Object> article = new HashMap<>();
                        article.put("article_id", post.getId().toString());
                        article.put("title", post.getTitle());
                        article.put("content", resolveContent(post));
                        article.put("tags", post.getTags() != null
                                ? Arrays.asList(post.getTags().split(","))
                                : List.of());
                        return article;
                    })
                    .collect(Collectors.toList());

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("articles", articles);

            boolean batchOk = false;
            for (int retry = 0; retry < MAX_RETRY && !batchOk; retry++) {
                try {
                    Map<String, Object> response = webClient.post()
                            .uri(aiServiceUrl + "/ingest/bulk")
                            .bodyValue(requestBody)
                            .retrieve()
                            .bodyToMono(Map.class)
                            .retryWhen(Retry.backoff(1, Duration.ofSeconds(1))
                                    .maxBackoff(Duration.ofSeconds(3)))
                            .block(Duration.ofSeconds(120));

                    if (response != null && "success".equals(response.get("status"))) {
                        success += batch.size();
                        batchOk = true;
                    } else {
                        log.warn("批次 {} 导入异常: response={}", batchNum, response);
                    }
                } catch (Exception e) {
                    log.error("批次 {} 导入失败: error={}", batchNum, e.getMessage());
                }
                if (!batchOk && retry < MAX_RETRY - 1) {
                    try {
                        Thread.sleep(RETRY_BASE_DELAY_MS * (1L << retry));
                    } catch (InterruptedException ignored) {}
                }
            }

            if (!batchOk) {
                failed += batch.size();
            }

            if (batchNum % 3 == 0 || i + BATCH_SIZE >= total) {
                log.info("启动导入进度: {}/{}, 成功={}, 失败={}", end, total, success, failed);
            }
        }

        log.info("启动全量知识库导入完成: total={}, success={}, failed={}", total, success, failed);
    }

    @Override
    @Async("importTaskExecutor")
    public void syncArticle(Long postId) {
        Post post = postService.getById(postId);
        if (post == null) {
            log.warn("文章不存在，跳过增量同步: articleId={}", postId);
            return;
        }

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("article_id", post.getId().toString());
        requestBody.put("title", post.getTitle());
        requestBody.put("content", resolveContent(post));
        requestBody.put("tags", post.getTags() != null
                ? Arrays.asList(post.getTags().split(","))
                : List.of());

        try {
            webClient.post()
                    .uri(aiServiceUrl + "/ingest")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .toBodilessEntity()
                    .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
                            .maxBackoff(Duration.ofSeconds(5)))
                    .block(Duration.ofSeconds(30));
            log.info("文章增量同步成功: articleId={}", postId);
        } catch (Exception e) {
            log.error("文章增量同步失败: articleId={}, error={}", postId, e.getMessage());
        }
    }

    private boolean waitForAiServiceReady(int maxAttempts, long backoffMs) {
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                Map<String, Object> status = webClient.get()
                        .uri(aiServiceUrl + "/rag/status")
                        .retrieve()
                        .bodyToMono(Map.class)
                        .block(Duration.ofSeconds(5));
                if (status != null && Boolean.TRUE.equals(status.get("ready"))) {
                    return true;
                }
            } catch (Exception e) {
                log.debug("AI 服务未就绪，重试 ({}/{}): {}", attempt, maxAttempts, e.getMessage());
            }
            if (attempt < maxAttempts) {
                try {
                    Thread.sleep(backoffMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
        }
        return false;
    }

    private Integer fetchKnowledgeBaseDocumentCount() {
        try {
            Map<String, Object> status = webClient.get()
                    .uri(aiServiceUrl + "/rag/status")
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block(Duration.ofSeconds(10));
            if (status != null && status.get("document_count") instanceof Integer count) {
                return count;
            }
        } catch (Exception e) {
            log.error("获取知识库文档数失败: {}", e.getMessage());
        }
        return null;
    }
}
