package com.mockerview.repository;

import com.mockerview.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    
    @Query("SELECT u FROM User u WHERE u.name = :name AND u.email = :email AND u.isDeleted = 0 ORDER BY u.createdAt DESC LIMIT 1")
    Optional<User> findByNameAndEmail(@Param("name") String name, @Param("email") String email);
    
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
}
