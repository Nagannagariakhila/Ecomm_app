package com.acc.serviceImpl;

import com.acc.dto.CouponResult;
import com.acc.entity.Coupon;
import com.acc.repository.CouponRepository;
import com.acc.service.CouponService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class CouponServiceImpl implements CouponService {

    private final CouponRepository couponRepository;

    @Autowired
    public CouponServiceImpl(CouponRepository couponRepository) {
        this.couponRepository = couponRepository;
    }

    @Override
    public CouponResult validateAndApplyCoupon(String couponCode, Double cartTotal) {
        Optional<Coupon> optionalCoupon = couponRepository.findByCode(couponCode);

        if (optionalCoupon.isEmpty()) {
            return new CouponResult("Coupon not found.", 0.0, null);
        }

        Coupon coupon = optionalCoupon.get();
        LocalDateTime now = LocalDateTime.now();

        if (!coupon.isActive()) {
            return new CouponResult("Coupon is not active.", 0.0, null);
        }

        if (now.isBefore(coupon.getStartDate()) || now.isAfter(coupon.getEndDate())) {
            return new CouponResult("Coupon has expired or is not yet valid.", 0.0, null);
        }

        if (cartTotal < coupon.getMinCartValue()) {
            return new CouponResult("Minimum cart value not met.", 0.0, null);
        }

        if (coupon.getUsageLimit() != null && coupon.getTimesUsed() >= coupon.getUsageLimit()) {
            return new CouponResult("Coupon usage limit reached.", 0.0, null);
        }

        Double discountAmount = calculateDiscount(coupon, cartTotal);
        coupon.setTimesUsed(coupon.getTimesUsed() + 1);
        couponRepository.save(coupon);

        return new CouponResult("Coupon applied successfully.", discountAmount, coupon.getCode());
    }

    private Double calculateDiscount(Coupon coupon, Double cartTotal) {
        if ("PERCENTAGE".equalsIgnoreCase(coupon.getDiscountType())) {
            return cartTotal * (coupon.getDiscountValue() / 100);
        } else if ("FIXED_AMOUNT".equalsIgnoreCase(coupon.getDiscountType())) {
            return coupon.getDiscountValue();
        }
        return 0.0;
    }

    @Override
    public List<Coupon> getAllCoupons() {
        return couponRepository.findAll();
    }

    @Override
    public Coupon createCoupon(Coupon coupon) {
        return couponRepository.save(coupon);
    }

    @Override
    public Optional<Coupon> getCouponById(Long id) {
        return couponRepository.findById(id);
    }

    @Override
    public Optional<Coupon> getCouponByCode(String code) {
        return couponRepository.findByCode(code);
    }

    @Override
    public Coupon updateCoupon(Long id, Coupon updatedCoupon) {
        Optional<Coupon> optionalCoupon = couponRepository.findById(id);
        if (optionalCoupon.isPresent()) {
            Coupon existingCoupon = optionalCoupon.get();
            existingCoupon.setCode(updatedCoupon.getCode());
            existingCoupon.setDiscountType(updatedCoupon.getDiscountType());
            existingCoupon.setDiscountValue(updatedCoupon.getDiscountValue());
            existingCoupon.setStartDate(updatedCoupon.getStartDate());
            existingCoupon.setEndDate(updatedCoupon.getEndDate());
            existingCoupon.setMinCartValue(updatedCoupon.getMinCartValue());
            existingCoupon.setActive(updatedCoupon.isActive());
            existingCoupon.setUsageLimit(updatedCoupon.getUsageLimit());
            existingCoupon.setOccasion(updatedCoupon.getOccasion());
            return couponRepository.save(existingCoupon);
        } else {
            throw new RuntimeException("Coupon not found with id: " + id);
        }
    }

    @Override
    public void deleteCoupon(Long id) {
        if (couponRepository.existsById(id)) {
            couponRepository.deleteById(id);
        } else {
            throw new RuntimeException("Coupon not found with id: " + id);
        }
    }
}