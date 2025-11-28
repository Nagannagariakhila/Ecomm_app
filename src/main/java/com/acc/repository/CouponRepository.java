package com.acc.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.acc.entity.Coupon;

import java.util.Optional;

public interface CouponRepository extends JpaRepository<Coupon, Long> {
    Optional<Coupon> findByCode(String code);
    boolean existsByCodeIgnoreCase(String code);
}