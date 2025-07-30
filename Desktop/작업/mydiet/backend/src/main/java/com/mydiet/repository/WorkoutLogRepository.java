package com.mydiet.mydiet.repository;

import com.mydiet.model.WorkoutLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface WorkoutLogRepository extends JpaRepository<WorkoutLog, Long> {
    List<WorkoutLog> findByUserIdAndDate(Long userId, LocalDate date);
    List<WorkoutLog> findByUserIdOrderByCreatedAtDesc(Long userId);
}
