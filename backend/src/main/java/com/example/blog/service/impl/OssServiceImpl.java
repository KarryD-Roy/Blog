package com.example.blog.service.impl;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.example.blog.config.OssProperties;
import com.example.blog.service.OssService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OssServiceImpl implements OssService {

    private final OssProperties ossProperties;

    @Override
    public String upload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("上传文件不能为空");
        }
        String endpoint = ossProperties.getEndpoint();
        String accessKeyId = ossProperties.getAccessKeyId();
        String accessKeySecret = ossProperties.getAccessKeySecret();
        String bucketName = ossProperties.getBucketName();

        if (!StringUtils.hasText(endpoint) || !StringUtils.hasText(accessKeyId)
                || !StringUtils.hasText(accessKeySecret) || !StringUtils.hasText(bucketName)) {
            throw new IllegalStateException("阿里云 OSS 配置信息不完整，请在 application.yml 中配置 aliyun.oss.*");
        }

        String originalFilename = file.getOriginalFilename();
        String suffix = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            suffix = originalFilename.substring(originalFilename.lastIndexOf("."));
        }

        String datePath = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE); // yyyyMMdd
        String key = "uploads/" + datePath + "/" + UUID.randomUUID().toString().replace("-", "") + suffix;

        OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
        try (InputStream inputStream = file.getInputStream()) {
            ossClient.putObject(bucketName, key, inputStream);
        } catch (Exception e) {
            throw new RuntimeException("上传到 OSS 失败", e);
        } finally {
            ossClient.shutdown();
        }

        String prefix = ossProperties.getPublicUrlPrefix();
        if (StringUtils.hasText(prefix)) {
            if (!prefix.endsWith("/")) {
                prefix = prefix + "/";
            }
            return prefix + key;
        }

        // 回退：拼接默认访问地址
        return "https://" + bucketName + "." + endpoint.replaceFirst("^https?://", "") + "/" + key;
    }
}

