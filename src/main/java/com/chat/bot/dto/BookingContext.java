package com.chat.bot.dto;

import lombok.Data;

@Data
public class BookingContext {
    private String name;
    private String service;
    private String location;
    private String date;
    private String time;
}