package com.acc.service;

import java.util.List;
import java.util.Optional;

import com.acc.dto.CouponResult;
import com.acc.entity.Coupon;

public interface CouponService {
    CouponResult validateAndApplyCoupon(String couponCode, Double cartTotal);

    List<Coupon> getAllCoupons();
    Coupon createCoupon(Coupon coupon);
    Coupon updateCoupon(Long id, Coupon updatedCoupon);
    void deleteCoupon(Long id);
    Optional<Coupon> getCouponById(Long id);
    Optional<Coupon> getCouponByCode(String code);
}