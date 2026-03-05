package com.chat.bot.service;

import com.chat.bot.dto.BookingContext;
import com.chat.bot.entity.BotState;
import com.chat.bot.entity.UserSession;
import com.chat.bot.repository.SessionRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class SessionService {

    private final SessionRepository sessionRepository;
    private final ObjectMapper objectMapper;

    // =================================
    // GET OR CREATE
    // =================================

    public UserSession getOrCreate(String phone) {
        return sessionRepository.findById(phone)
                .orElseGet(() -> create(phone));
    }

    private UserSession create(String phone) {
        UserSession session = new UserSession();
        session.setPhoneNumber(phone);
        session.setState(BotState.START);
        session.setLastActivity(LocalDateTime.now());
        return sessionRepository.save(session);
    }

    // =================================
    // TIMEOUT
    // =================================

    public boolean isTimedOut(UserSession session) {
        return session.getLastActivity() != null &&
                session.getLastActivity()
                        .isBefore(LocalDateTime.now().minusMinutes(10));
    }

    public void updateActivity(UserSession session) {
        session.setLastActivity(LocalDateTime.now());
    }

    // =================================
    // STATE
    // =================================

    public void updateState(UserSession session, BotState state) {
        session.setState(state);
    }

    public void reset(UserSession session) {
        session.setState(BotState.START);
        session.setContextJson(null);
        session.setLastActivity(LocalDateTime.now());
    }

    // =================================
    // CONTEXT
    // =================================

    public BookingContext getContext(UserSession session) {
        if (session.getContextJson() == null) {
            return new BookingContext();
        }

        try {
            return objectMapper.readValue(
                    session.getContextJson(),
                    BookingContext.class
            );
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Corrupted session context JSON", e);
        }
    }

    public void saveContext(UserSession session, BookingContext context) {
        try {
            session.setContextJson(
                    objectMapper.writeValueAsString(context)
            );
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize session context", e);
        }
    }
}