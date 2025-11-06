package com.mockerview.repository;

import com.mockerview.entity.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    @Query("SELECT u FROM User u WHERE u.username = :username AND u.isDeleted = 0 ORDER BY u.createdAt DESC")
    List<User> findByUsernameList(@Param("username") String username, Pageable pageable);
    
    default Optional<User> findByUsername(String username) {
        List<User> users = findByUsernameList(username, Pageable.ofSize(1));
        return users.isEmpty() ? Optional.empty() : Optional.of(users.get(0));
    }
    
    @Query("SELECT u FROM User u WHERE u.name = :name AND u.email = :email AND u.isDeleted = 0 ORDER BY u.createdAt DESC")
    List<User> findByNameAndEmailList(@Param("name") String name, @Param("email") String email, Pageable pageable);
    
    default Optional<User> findByNameAndEmail(String name, String email) {
        List<User> users = findByNameAndEmailList(name, email, Pageable.ofSize(1));
        return users.isEmpty() ? Optional.empty() : Optional.of(users.get(0));
    }
    
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);

    @Query("SELECT u FROM User u WHERE u.email = :email AND u.isDeleted = 0")
    Optional<User> findByEmail(@Param("email") String email);
    
    @Query("SELECT u FROM User u WHERE u.email = :email")
    Optional<User> findByEmailIncludingDeleted(@Param("email") String email);
    
    @Query("SELECT u FROM User u WHERE u.email = :email AND u.isDeleted = 0")
    Optional<User> findActiveByEmail(@Param("email") String email);
    
    @Query("SELECT u FROM User u WHERE u.username = :username AND u.isDeleted = 0")
    Optional<User> findActiveByUsername(@Param("username") String username);
    
    @Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END FROM User u WHERE u.email = :email AND u.isDeleted = 0")
    boolean existsActiveByEmail(@Param("email") String email);
    
    @Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END FROM User u WHERE u.username = :username AND u.isDeleted = 0")
    boolean existsActiveByUsername(@Param("username") String username);
    
    @Query("SELECT COUNT(u) FROM User u WHERE u.role = :role AND u.isDeleted = 0")
    long countByRole(@Param("role") User.UserRole role);
    
    @Query("SELECT u FROM User u WHERE u.role = :role AND u.isDeleted = 0")
    List<User> findByRole(@Param("role") User.UserRole role);
}