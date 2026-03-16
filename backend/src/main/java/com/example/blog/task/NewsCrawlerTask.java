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
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class NewsCrawlerTask {

    private final HotNewsService hotNewsService;

    /**
     * 每天早上 8 点抓取一次
     */
    @Scheduled(cron = "0 0 8 * * ?")
    public void crawlDailyNews() {
        log.info("开始抓取每日技术热点...");
        List<HotNews> newsList = new ArrayList<>();

        // 尝试抓取 Hacker News (作为示例，实际可替换为国内源如 OSChina, Juejin 等)
        //由于国内访问不稳定，这里采用模拟数据+简单抓取混合策略
        // 如果能访问 Hacker News 则抓取，否则生成模拟数据
        try {
            // 设置超时时间，避免卡住
            Document doc = Jsoup.connect("https://news.ycombinator.com/")
                    .timeout(5000)
                    .userAgent("Mozilla/5.0")
                    .get();

            Elements titleElements = doc.select(".titleline > a");
            int count = 0;
            for (Element el : titleElements) {
                if (count >= 5) break;
                String title = el.text();
                String url = el.attr("href");

                HotNews news = new HotNews();
                news.setTitle(title); // 实际项目中可接入翻译 API
                news.setUrl(url);
                news.setSource("Hacker News");
                news.setPublishDate(LocalDateTime.now());
                // 随机图或默认图
                news.setImageUrl("https://picsum.photos/seed/" + System.currentTimeMillis() + count + "/800/420");

                newsList.add(news);
                count++;
            }
        } catch (Exception e) {
            log.error("抓取 Hacker News 失败，切换为生成模拟数据", e);
            // 生成兜底数据
            generateMockNews(newsList);
        }

        if (newsList.isEmpty()) {
            generateMockNews(newsList);
        }

        // 保存数据库
        for (HotNews news : newsList) {
            // 简单去重：检查今日是否已存在同名/同URL的新闻
            // 这里简化处理，直接插入
            hotNewsService.save(news);
        }

        log.info("每日热点抓取完成，新增 {} 条", newsList.size());
    }

    private void generateMockNews(List<HotNews> list) {
        String dateStr = LocalDateTime.now().toLocalDate().toString();

        HotNews n1 = new HotNews();
        n1.setTitle("技术日报 [" + dateStr + "]: Spring Boot 3.4 发布预览");
        n1.setUrl("https://spring.io/blog");
        n1.setSource("Spring Blog");
        n1.setPublishDate(LocalDateTime.now());
        n1.setImageUrl("https://picsum.photos/seed/spring/800/420");
        list.add(n1);

        HotNews n2 = new HotNews();
        n2.setTitle("每日AI动态: GPT-5 传闻与最新进展");
        n2.setUrl("https://openai.com/blog");
        n2.setSource("AI News");
        n2.setPublishDate(LocalDateTime.now());
        n2.setImageUrl("https://picsum.photos/seed/ai/800/420");
        list.add(n2);

        HotNews n3 = new HotNews();
        n3.setTitle("前端趋势: Vue 3.5 带来的新特性");
        n3.setUrl("https://blog.vuejs.org/");
        n3.setSource("Vue Blog");
        n3.setPublishDate(LocalDateTime.now());
        n3.setImageUrl("https://picsum.photos/seed/vue/800/420");
        list.add(n3);
    }
}
