package com.chat.bot.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class BookingStatsDTO {
    private long todayCount;
    private long thisMonthCount;
    private long thisYearCount;
}
