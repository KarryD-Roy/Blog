package com.example.blog.service;

import org.springframework.web.multipart.MultipartFile;

public interface OssService {

    /**
     * 上传文件到 OSS，返回可公网访问的 URL
     */
    String upload(MultipartFile file);
}

