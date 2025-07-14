package com.acc.repository;

import com.acc.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> { // Assuming Role ID is Long

    Optional<Role> findByName(String name);
}