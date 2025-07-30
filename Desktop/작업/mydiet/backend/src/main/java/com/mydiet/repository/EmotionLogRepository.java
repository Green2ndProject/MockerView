package com.mydiet.repository;

import com.mydiet.model.EmotionLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface EmotionLogRepository extends JpaRepository<EmotionLog, Long> {
    List<EmotionLog> findByUserIdAndDate(Long userId, LocalDate date);
}
