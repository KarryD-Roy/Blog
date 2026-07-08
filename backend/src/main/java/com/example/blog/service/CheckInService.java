package com.example.blog.service;

import com.example.blog.dto.CheckInVo;

public interface CheckInService {
    CheckInVo checkIn(Long userId);
    CheckInVo getCheckInStatus(Long userId);
}
