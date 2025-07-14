package com.acc.repository;

import com.acc.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import jakarta.persistence.LockModeType;

import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    Optional<Customer> findByUsername(String username);

    Optional<Customer> findByEmail(String email);

    Optional<Customer> findTopByOrderByIdDesc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(value = "SELECT MAX(CAST(SUBSTRING(customer_code, 3) AS UNSIGNED)) FROM customer WHERE customer_code LIKE 'CH%'", nativeQuery = true)
    Integer findMaxCustomerCodeNumberWithLock();

    boolean existsByCustomerCode(String customerCode);
}
