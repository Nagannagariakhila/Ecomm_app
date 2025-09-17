package com.acc.entity;

import java.math.BigDecimal;

public class InvoiceItem {

    private String productName;
    private int quantity;
    private double Price;
    private BigDecimal discountedAmount; 

    private double subtotal;

    
    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public double getUnitPrice() {
        return Price;
    }

    public void setUnitPrice(double unitPrice) {
        this.Price = unitPrice;
    }

    public double getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(double subtotal) {
        this.subtotal = subtotal;
    }
}