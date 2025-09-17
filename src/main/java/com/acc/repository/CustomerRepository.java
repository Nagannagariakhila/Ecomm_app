package com.acc.repository;

import com.acc.entity.Customer;
import com.acc.entity.Order;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    Optional<Customer> findByUsername(String username);

    Optional<Customer> findByEmail(String email);

    Optional<Customer> findTopByOrderByIdDesc();

    boolean existsByCustomerCode(String customerCode);

	Optional<Order> findByCustomerCode(String customerCode);
    
    
}

