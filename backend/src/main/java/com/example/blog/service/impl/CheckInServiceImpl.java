package com.example.blog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.blog.dto.CheckInVo;
import com.example.blog.entity.CheckInRecord;
import com.example.blog.mapper.CheckInMapper;
import com.example.blog.service.CheckInService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
public class CheckInServiceImpl implements CheckInService {

    private final CheckInMapper checkInMapper;

    public CheckInServiceImpl(CheckInMapper checkInMapper) {
        this.checkInMapper = checkInMapper;
    }

    @Override
    @Transactional
    public CheckInVo checkIn(Long userId) {
        LocalDate today = LocalDate.now();
        CheckInVo vo = new CheckInVo();
        
        LambdaQueryWrapper<CheckInRecord> todayQuery = new LambdaQueryWrapper<>();
        todayQuery.eq(CheckInRecord::getUserId, userId)
                  .eq(CheckInRecord::getCheckinDate, today);
        Long existingCount = checkInMapper.selectCount(todayQuery);
        
        if (existingCount != null && existingCount > 0) {
            vo.setCheckedToday(true);
            vo.setMessage("今日已签到，请明天再来");
            fillCheckInStatus(userId, vo);
            return vo;
        }
        
        // Calculate consecutive days
        String lastDateStr = checkInMapper.getLastCheckInDate(userId);
        int consecutiveDays = 1;
        
        if (lastDateStr != null && !lastDateStr.isEmpty()) {
            LocalDate lastDate = LocalDate.parse(lastDateStr);
            LocalDate yesterday = today.minusDays(1);
            
            if (lastDate.equals(yesterday)) {
                Integer prevConsecutive = checkInMapper.getLatestConsecutiveDays(userId);
                consecutiveDays = (prevConsecutive != null ? prevConsecutive : 0) + 1;
            } else if (lastDate.isBefore(yesterday)) {
                consecutiveDays = 1;
            }
        }
        
        CheckInRecord record = new CheckInRecord();
        record.setUserId(userId);
        record.setCheckinDate(today);
        record.setConsecutiveDays(consecutiveDays);
        record.setCreatedAt(LocalDateTime.now());
        checkInMapper.insert(record);
        
        vo.setCheckedToday(true);
        vo.setMessage("签到成功！连续签到 " + consecutiveDays + " 天");
        fillCheckInStatus(userId, vo);
        return vo;
    }

    @Override
    public CheckInVo getCheckInStatus(Long userId) {
        CheckInVo vo = new CheckInVo();
        fillCheckInStatus(userId, vo);
        return vo;
    }
    
    private void fillCheckInStatus(Long userId, CheckInVo vo) {
        LocalDate today = LocalDate.now();
        
        LambdaQueryWrapper<CheckInRecord> todayQuery = new LambdaQueryWrapper<>();
        todayQuery.eq(CheckInRecord::getUserId, userId)
                  .eq(CheckInRecord::getCheckinDate, today);
        Long todayCount = checkInMapper.selectCount(todayQuery);
        vo.setCheckedToday(todayCount != null && todayCount > 0);
        
        Integer consecutive = checkInMapper.getLatestConsecutiveDays(userId);
        vo.setConsecutiveDays(consecutive != null ? consecutive : 0);
        
        vo.setTotalCheckIns(checkInMapper.countByUserId(userId));
        vo.setLastCheckInDate(checkInMapper.getLastCheckInDate(userId));
    }
}
