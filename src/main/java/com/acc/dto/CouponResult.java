package com.acc.dto;
public class CouponResult {
    private String message;
    private Double discountAmount;
    private String couponCode;

    public CouponResult() {
    }

    public CouponResult(String message, Double discountAmount, String couponCode) {
        this.message = message;
        this.discountAmount = discountAmount;
        this.couponCode = couponCode;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Double getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(Double discountAmount) {
        this.discountAmount = discountAmount;
    }

    public String getCouponCode() {
        return couponCode;
    }

    public void setCouponCode(String couponCode) {
        this.couponCode = couponCode;
    }
}