package com.example.blog.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "aliyun.oss")
public class OssProperties {

    /**
     * 例如：https://oss-cn-hangzhou.aliyuncs.com
     */
    private String endpoint;

    private String accessKeyId;

    private String accessKeySecret;

    /**
     * Bucket 名称
     */
    private String bucketName;

    /**
     * 公网访问前缀，例如：https://your-bucket.oss-cn-hangzhou.aliyuncs.com/
     */
    private String publicUrlPrefix;
}

