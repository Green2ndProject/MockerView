package com.mockerview.repository;

import com.mockerview.entity.QuestionPool;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface QuestionPoolRepository extends JpaRepository<QuestionPool, Long> {
    List<QuestionPool> findByCategory(String category);
    List<QuestionPool> findByDifficulty(String difficulty);
    
    @Query(value = "SELECT * FROM (SELECT * FROM question_pool WHERE CATEGORY = ?1 ORDER BY DBMS_RANDOM.VALUE) WHERE ROWNUM <= 10", nativeQuery = true)
    List<QuestionPool> findRandomByCategory(String category);
    
    @Query(value = "SELECT * FROM (SELECT * FROM question_pool ORDER BY DBMS_RANDOM.VALUE) WHERE ROWNUM <= 10", nativeQuery = true)
    List<QuestionPool> findAllRandom();
    
    @Query(value = "SELECT * FROM (SELECT * FROM question_pool ORDER BY DBMS_RANDOM.VALUE) WHERE ROWNUM <= :count", nativeQuery = true)
    List<QuestionPool> findRandomQuestions(@Param("count") Integer count);
    
    @Query(value = "SELECT * FROM (SELECT * FROM question_pool WHERE difficulty = :difficulty AND category = :category ORDER BY DBMS_RANDOM.VALUE) WHERE ROWNUM <= :limit", nativeQuery = true)
    List<QuestionPool> findRandomQuestionsByDifficultyAndCategory(
        @Param("difficulty") String difficulty,
        @Param("category") String category,
        @Param("limit") Integer limit
    );
    
    @Query(value = "SELECT * FROM (SELECT * FROM question_pool WHERE difficulty = :difficulty ORDER BY DBMS_RANDOM.VALUE) WHERE ROWNUM <= :limit", nativeQuery = true)
    List<QuestionPool> findRandomQuestionsByDifficulty(
        @Param("difficulty") String difficulty,
        @Param("limit") Integer limit
    );
    
    @Query(value = "SELECT * FROM (SELECT * FROM question_pool WHERE category = :category ORDER BY DBMS_RANDOM.VALUE) WHERE ROWNUM <= :limit", nativeQuery = true)
    List<QuestionPool> findRandomQuestionsByCategory(
        @Param("category") String category,
        @Param("limit") Integer limit
    );
}