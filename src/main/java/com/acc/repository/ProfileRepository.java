package com.acc.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.acc.entity.Customer;
import com.acc.entity.Profile;

import java.util.Optional; // <-- Import Optional

@Repository
public interface ProfileRepository extends JpaRepository<Profile, Long> {
        Optional<Profile> findByCustomerId(Long customerId);

		boolean existsByCustomer(Customer customer);
}