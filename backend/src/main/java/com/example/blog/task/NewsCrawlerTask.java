package com.example.blog.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.blog.entity.HotNews;
import com.example.blog.service.HotNewsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class NewsCrawlerTask {

    private final HotNewsService hotNewsService;

    private static final int MAX_FETCH = 5;
    private static final int MAX_RETRY = 3;
    private static final int TIMEOUT_MS = 15000;
    private static final int CIRCUIT_FAIL_THRESHOLD = 3;
    private static final Duration CIRCUIT_COOLDOWN = Duration.ofMinutes(5);

    private int consecutiveFailures = 0;
    private LocalDateTime lastFailureTime = null;

    @Async
    @EventListener(ApplicationReadyEvent.class)
    public void crawlOnStartup() {
        crawlDailyNews();
    }

    /**
     * 每天早上 8 点抓取一次
     */
    @CacheEvict(cacheNames = "hotNews", allEntries = true)
    @Scheduled(cron = "0 0 8 * * ?")
    public void crawlDailyNews() {
        // 短期熔断保护：连续失败达到阈值，且未过冷却时间则跳过
        if (consecutiveFailures >= CIRCUIT_FAIL_THRESHOLD && lastFailureTime != null) {
            Duration sinceLastFail = Duration.between(lastFailureTime, LocalDateTime.now());
            if (sinceLastFail.compareTo(CIRCUIT_COOLDOWN) < 0) {
                log.warn("抓取已熔断，距上次失败 {} 秒，等待冷却结束", sinceLastFail.getSeconds());
                return;
            } else {
                consecutiveFailures = 0;
            }
        }

        log.info("开始抓取每日技术热点...");
        List<HotNews> newsList = new ArrayList<>();

        try {
            Document doc = fetchWithRetry("https://www.oschina.net/news", MAX_RETRY);

            Elements items = doc.select(".news-list-container .item");
            int count = 0;
            for (Element item : items) {
                if (count >= MAX_FETCH) break;

                String title = "";
                String url = item.attr("data-url");

                Element titleEl = item.select(".title").first();
                if (titleEl != null) {
                    title = titleEl.text();
                } else {
                    Element headerLink = item.select(".content .header a").first();
                    if (headerLink != null) {
                        title = headerLink.text();
                        if (url == null || url.isEmpty()) {
                            url = headerLink.attr("href");
                        }
                    }
                }

                if (title != null && !title.isEmpty()) {
                    if (url != null && !url.startsWith("http")) {
                        url = "https://www.oschina.net" + url;
                    }

                    HotNews news = new HotNews();
                    news.setTitle(title);
                    news.setUrl(url);
                    news.setSource("OSChina");
                    news.setPublishDate(LocalDateTime.now());
                    news.setImageUrl("https://picsum.photos/seed/" + (title.hashCode()) + "/800/450");

                    newsList.add(news);
                    count++;
                }
            }

            consecutiveFailures = 0;
            lastFailureTime = null;
        } catch (Exception e) {
            consecutiveFailures++;
            lastFailureTime = LocalDateTime.now();
            log.error("抓取 OSChina 失败，切换为生成模拟数据 (连续失败: {})", consecutiveFailures, e);
            generateMockNews(newsList);
        }

        if (newsList.isEmpty()) {
            generateMockNews(newsList);
        }

        hotNewsService.remove(new LambdaQueryWrapper<HotNews>());
        for (HotNews news : newsList) {
            hotNewsService.save(news);
        }

        log.info("每日热点抓取完成，新增 {} 条", newsList.size());
    }

    private Document fetchWithRetry(String url, int maxRetries) throws Exception {
        Exception last = null;
        for (int i = 1; i <= maxRetries; i++) {
            try {
                return Jsoup.connect(url)
                        .timeout(TIMEOUT_MS)
                        .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                        .header("Referer", "https://www.oschina.net/")
                        .header("Accept-Language", "zh-CN,zh;q=0.9")
                        .header("Cache-Control", "no-cache")
                        .get();
            } catch (Exception e) {
                last = e;
                log.warn("抓取失败，第 {}/{} 次重试: {}", i, maxRetries, e.getMessage());
                Thread.sleep(1200L * i);
            }
        }
        throw last;
    }

    private void generateMockNews(List<HotNews> list) {
        String dateStr = LocalDateTime.now().toLocalDate().toString();

        HotNews n1 = new HotNews();
        n1.setTitle("技术日报 [" + dateStr + "]: Spring Boot 3.4 发布预览");
        n1.setUrl("https://spring.io/blog");
        n1.setSource("Spring Blog");
        n1.setPublishDate(LocalDateTime.now());
        n1.setImageUrl("https://picsum.photos/seed/spring/800/450");
        list.add(n1);

        HotNews n2 = new HotNews();
        n2.setTitle("每日AI动态: GPT-5 传闻与最新进展");
        n2.setUrl("https://openai.com/blog");
        n2.setSource("AI News");
        n2.setPublishDate(LocalDateTime.now());
        n2.setImageUrl("https://picsum.photos/seed/ai/800/450");
        list.add(n2);

        HotNews n3 = new HotNews();
        n3.setTitle("前端趋势: Vue 3.5 带来的新特性");
        n3.setUrl("https://blog.vuejs.org/");
        n3.setSource("Vue Blog");
        n3.setPublishDate(LocalDateTime.now());
        n3.setImageUrl("https://picsum.photos/seed/vue/800/450");
        list.add(n3);
    }
}
