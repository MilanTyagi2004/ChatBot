package com.chat.bot.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_sessions")
@Getter
@Setter
public class UserSession {

    @Id
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    private BotState state;

    @Column(columnDefinition = "TEXT")
    private String contextJson;

    private LocalDateTime lastActivity;
}