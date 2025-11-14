package com.mockerview.repository;

import com.mockerview.entity.QuestionPool;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuestionPoolRepository extends JpaRepository<QuestionPool, Long> {
    
    @Query("SELECT CASE WHEN COUNT(qp) > 0 THEN true ELSE false END " +
           "FROM QuestionPool qp " +
           "WHERE qp.text = :text AND qp.category = :category")
    boolean existsByTextAndCategory(@Param("text") String text, @Param("category") String category);
    
    List<QuestionPool> findByCategory(String category);
    
    List<QuestionPool> findByCategoryAndDifficulty(String category, String difficulty);
    
    @Query("SELECT COUNT(qp) FROM QuestionPool qp")
    long countAll();
}
