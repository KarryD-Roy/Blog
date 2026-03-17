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
import org.springframework.context.event.EventListener;
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
    @EventListener(ApplicationReadyEvent.class) // 应用启动时立即抓取一次
    public void crawlDailyNews() {
        log.info("开始抓取每日技术热点...");
        List<HotNews> newsList = new ArrayList<>();

        // 尝试抓取 OSChina (开源中国)
        try {
            // 设置超时时间，模拟浏览器 UA
            Document doc = Jsoup.connect("https://www.oschina.net/news")
                    .timeout(5000)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .header("Referer", "https://www.oschina.net/")
                    .get();

            // OSChina 新闻列表通常在 .news-list-container .item 中
            Elements items = doc.select(".news-list-container .item");
            int count = 0;
            for (Element item : items) {
                if (count >= 5) break;

                // 提取标题和链接
                String title = "";
                String url = item.attr("data-url");

                Element titleEl = item.select(".title").first();
                if (titleEl != null) {
                    title = titleEl.text();
                } else {
                    // 备用选择器
                    Element headerLink = item.select(".content .header a").first();
                    if (headerLink != null) {
                        title = headerLink.text();
                        if (url == null || url.isEmpty()) {
                            url = headerLink.attr("href");
                        }
                    }
                }

                if (title != null && !title.isEmpty()) {
                    // 处理相对链接
                    if (url != null && !url.startsWith("http")) {
                        url = "https://www.oschina.net" + url;
                    }

                    HotNews news = new HotNews();
                    news.setTitle(title);
                    news.setUrl(url);
                    news.setSource("OSChina");
                    news.setPublishDate(LocalDateTime.now());
                    // 替换为更稳定的 Lorem Picsum 或国外更快的静态图服务，确保在不同网络下更有保障
                    // 使用 Picsum 的种子功能来为每条新闻获取相对客观的随机图
                    news.setImageUrl("https://picsum.photos/seed/" + (title.hashCode()) + "/800/450");

                    newsList.add(news);
                    count++;
                }
            }
        } catch (Exception e) {
            log.error("抓取 OSChina 失败，切换为生成模拟数据", e);
            // 生成兜底数据
            generateMockNews(newsList);
        }

        if (newsList.isEmpty()) {
            generateMockNews(newsList);
        }

        // 清理旧数据，只保留最新的，防止数据无限增长
        hotNewsService.remove(new LambdaQueryWrapper<HotNews>());

        // 保存数据库
        for (HotNews news : newsList) {
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
