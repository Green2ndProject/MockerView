package com.mockerview.repository;

import com.mockerview.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    
    Optional<Category> findByCode(String code);
    
    @Query("SELECT c FROM Category c WHERE c.parent IS NULL AND c.isActive = true ORDER BY c.displayOrder")
    List<Category> findAllRootCategories();
    
    @Query("SELECT c FROM Category c WHERE c.parent.id = :parentId AND c.isActive = true ORDER BY c.displayOrder")
    List<Category> findByParentId(@Param("parentId") Long parentId);
    
    @Query("SELECT c FROM Category c WHERE c.categoryType = 'MAIN' AND c.isActive = true ORDER BY c.displayOrder")
    List<Category> findAllMainCategories();
    
    @Query("SELECT c FROM Category c WHERE c.parent.code = :parentCode AND c.isActive = true ORDER BY c.displayOrder")
    List<Category> findSubCategoriesByParentCode(@Param("parentCode") String parentCode);
    
    List<Category> findByIsActiveTrueOrderByDisplayOrder();
}
