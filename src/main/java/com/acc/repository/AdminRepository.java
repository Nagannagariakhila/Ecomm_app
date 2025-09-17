package com.acc.repository;
import com.acc.entity.Admin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.Optional;
@Repository
public interface AdminRepository extends JpaRepository<Admin, Long> {
    @Query("SELECT a FROM Admin a LEFT JOIN FETCH a.roles WHERE a.username = :username")

    Optional<Admin> findByUsername(String username);
    
    @Query("SELECT a FROM Admin a LEFT JOIN FETCH a.roles WHERE a.email = :email")

    Optional<Admin> findByEmail(String email);
}
