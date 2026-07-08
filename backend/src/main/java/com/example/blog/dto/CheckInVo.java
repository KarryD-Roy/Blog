package com.example.blog.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CheckInVo {
    private boolean checkedToday;
    private int consecutiveDays;
    private int totalCheckIns;
    private String lastCheckInDate;
    private String message;
}
