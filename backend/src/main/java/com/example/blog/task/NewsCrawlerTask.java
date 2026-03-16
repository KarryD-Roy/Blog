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

        // 尝试抓取 OSChina (开源中国)
        try {
            // 设置超时时间，模拟浏览器 UA
            Document doc = Jsoup.connect("https://www.oschina.net/news")
                    .timeout(5000)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .header("Referer", "https://www.oschina.net/")
                    .get();

            // OSChina 新闻列表通常在 .news-list .item 中
            Elements items = doc.select(".news-list .item");
            int count = 0;
            for (Element item : items) {
                if (count >= 5) break;

                // 提取标题链接，常见结构为 .content .header a
                Element titleEl = item.select(".content .header a").first();
                // 备用选择器
                if (titleEl == null) {
                    titleEl = item.select(".title").first();
                }

                if (titleEl != null) {
                    String title = titleEl.text();
                    String url = titleEl.attr("href");

                    // 处理相对链接
                    if (url != null && !url.startsWith("http")) {
                        url = "https://www.oschina.net" + url;
                    }

                    HotNews news = new HotNews();
                    news.setTitle(title);
                    news.setUrl(url);
                    news.setSource("OSChina");
                    news.setPublishDate(LocalDateTime.now());
                    // 随机图 (使用国内可访问的随机风景图 API，例如韩小韩 API)
                    // 添加随机参数以区分列表中的不同图片
                    news.setImageUrl("https://api.vvhan.com/api/wallpaper/views?type=302&t=" + (System.currentTimeMillis() + count));

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

        // 保存数据库
        for (HotNews news : newsList) {
            hotNewsService.save(news);
        }

        log.info("每日热点抓取完成，新增 {} 条", newsList.size());
    }

    private void generateMockNews(List<HotNews> list) {
        String dateStr = LocalDateTime.now().toLocalDate().toString();
        long ts = System.currentTimeMillis();

        HotNews n1 = new HotNews();
        n1.setTitle("技术日报 [" + dateStr + "]: Spring Boot 3.4 发布预览");
        n1.setUrl("https://spring.io/blog");
        n1.setSource("Spring Blog");
        n1.setPublishDate(LocalDateTime.now());
        n1.setImageUrl("https://api.vvhan.com/api/wallpaper/views?type=302&t=" + ts);
        list.add(n1);

        HotNews n2 = new HotNews();
        n2.setTitle("每日AI动态: GPT-5 传闻与最新进展");
        n2.setUrl("https://openai.com/blog");
        n2.setSource("AI News");
        n2.setPublishDate(LocalDateTime.now());
        n2.setImageUrl("https://api.vvhan.com/api/wallpaper/views?type=302&t=" + (ts + 1));
        list.add(n2);

        HotNews n3 = new HotNews();
        n3.setTitle("前端趋势: Vue 3.5 带来的新特性");
        n3.setUrl("https://blog.vuejs.org/");
        n3.setSource("Vue Blog");
        n3.setPublishDate(LocalDateTime.now());
        n3.setImageUrl("https://api.vvhan.com/api/wallpaper/views?type=302&t=" + (ts + 2));
        list.add(n3);
    }
}
