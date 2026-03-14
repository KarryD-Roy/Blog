package com.example.blog.controller;

import com.example.blog.service.OssService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/oss")
@RequiredArgsConstructor
public class OssController {

    private final OssService ossService;

    @PostMapping("/upload")
    public ApiResponse<String> upload(@RequestPart("file") MultipartFile file) {
        String url = ossService.upload(file);
        return ApiResponse.ok(url);
    }
}

