package com.chat.bot.repository;

import com.chat.bot.entity.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SessionRepository extends JpaRepository<UserSession,String> {
}
