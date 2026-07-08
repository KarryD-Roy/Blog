package com.example.blog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.blog.entity.CheckInRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface CheckInMapper extends BaseMapper<CheckInRecord> {
    
    @Select("SELECT COUNT(*) FROM checkin_records WHERE user_id = #{userId}")
    int countByUserId(Long userId);
    
    @Select("SELECT IFNULL(MAX(checkin_date), NULL) FROM checkin_records WHERE user_id = #{userId}")
    String getLastCheckInDate(Long userId);
    
    @Select("SELECT consecutive_days FROM checkin_records WHERE user_id = #{userId} ORDER BY checkin_date DESC LIMIT 1")
    Integer getLatestConsecutiveDays(Long userId);
}
