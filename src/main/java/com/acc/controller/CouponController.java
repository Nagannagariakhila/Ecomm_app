package com.acc.controller;

import com.acc.dto.CouponDto;
import com.acc.dto.CouponResult;
import com.acc.dto.CouponValidation;
import com.acc.entity.Coupon;
import com.acc.service.CouponService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/coupons")
@CrossOrigin(origins = "http://localhost:4200")
public class CouponController {

    private final CouponService couponService;

    @Autowired
    public CouponController(CouponService couponService) {
        this.couponService = couponService;
    }

    @GetMapping
    public List<CouponDto> getAllCoupons() {
        return couponService.getAllCoupons().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @PostMapping("/validate")
    public ResponseEntity<CouponResult> validateCoupon(@RequestBody CouponValidation request) {
        CouponResult response = couponService.validateAndApplyCoupon(request.getCouponCode(), request.getCartTotal());
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<CouponDto> createCoupon(@RequestBody CouponDto couponDto) {
        Coupon coupon = convertToEntity(couponDto);
        Coupon createdCoupon = couponService.createCoupon(coupon);
        return ResponseEntity.ok(convertToDto(createdCoupon));
    }
    
    


    @PutMapping("/{id}")
    public ResponseEntity<CouponDto> updateCoupon(@PathVariable Long id, @RequestBody CouponDto couponDto) {
        try {
            Coupon updatedCoupon = couponService.updateCoupon(id, convertToEntity(couponDto));
            return ResponseEntity.ok(convertToDto(updatedCoupon));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCoupon(@PathVariable Long id) {
        try {
            couponService.deleteCoupon(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<CouponDto> getCouponById(@PathVariable Long id) {
        Optional<Coupon> coupon = couponService.getCouponById(id);
        return coupon.map(this::convertToDto)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/code/{code}")
    public ResponseEntity<CouponDto> getCouponByCode(@PathVariable String code) {
        Optional<Coupon> coupon = couponService.getCouponByCode(code);
        return coupon.map(this::convertToDto)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
    
    @PostMapping("/apply")
    public ResponseEntity<CouponResult> applyCoupon(@RequestBody CouponValidation request) {
        try {
            CouponResult result = couponService.validateAndApplyCoupon(request.getCouponCode(), request.getCartTotal());
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            // This handles cases like an invalid or expired coupon
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new CouponResult(e.getMessage(), 0.0, request.getCouponCode()));
        }
    }

    private CouponDto convertToDto(Coupon coupon) {
        CouponDto dto = new CouponDto();
        dto.setId(coupon.getId());
        dto.setCode(coupon.getCode());
        dto.setDiscountType(coupon.getDiscountType());
        dto.setDiscountValue(coupon.getDiscountValue());
        dto.setStartDate(coupon.getStartDate());
        dto.setEndDate(coupon.getEndDate());
        dto.setMinCartValue(coupon.getMinCartValue());
        dto.setActive(coupon.isActive());
        dto.setUsageLimit(coupon.getUsageLimit());
        dto.setTimesUsed(coupon.getTimesUsed());
        dto.setOccasion(coupon.getOccasion());
        return dto;
    }

    private Coupon convertToEntity(CouponDto dto) {
        Coupon coupon = new Coupon();
        coupon.setId(dto.getId());
        coupon.setCode(dto.getCode());
        coupon.setDiscountType(dto.getDiscountType());
        coupon.setDiscountValue(dto.getDiscountValue());
        coupon.setStartDate(dto.getStartDate());
        coupon.setEndDate(dto.getEndDate());
        coupon.setMinCartValue(dto.getMinCartValue());
        coupon.setActive(dto.isActive());
        coupon.setUsageLimit(dto.getUsageLimit());
        coupon.setTimesUsed(dto.getTimesUsed() != null ? dto.getTimesUsed() : 0);
        coupon.setOccasion(dto.getOccasion());
        return coupon;
    }
}
