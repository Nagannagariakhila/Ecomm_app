package com.acc.dto;

import java.time.LocalDateTime;
import java.math.BigDecimal; 
import java.util.List;

public class OrderDTO {
    private Long id; 
    private String customerCode;
    private String orderCode;
    
    private LocalDateTime orderDate;
    private BigDecimal totalAmount; 
    private String status; 
    private List<OrderItemDTO> orderItems;
    private Long customerId; 
    private String customerUsername;
    private Long addressId;
    private String shippingAddressString; 
    private String customerFirstName; 
    private String customerLastName; 
    private BigDecimal discountAmount; 
    private BigDecimal discountedAmount; 

    public String getCustomerCode() {
    	return customerCode;
    }
    public void setCustomerCode(String customerCode) {
		this.customerCode = customerCode;
	}
    public String getCustomerFirstName() {
    	return customerFirstName;
    }
    public void setCustomerFirstName(String customerFirstName) {
		this.customerFirstName = customerFirstName;
	}
    public String getCustomerLastName() {
		return customerLastName;
	}
    	public void setCustomerLastName(String customerLastName) {
    		this.customerLastName = customerLastName;
    	}
    public String getShippingAddressString() {
		return shippingAddressString;
	}
    public void setShippingAddressString(String shippingAddressString) {
    	this.shippingAddressString = shippingAddressString;
    	
    }
    public String getOrderCode() {
        return orderCode;
    }

    public void setOrderCode(String orderCode) {
        this.orderCode = orderCode;
    }

    public Long getAddressId() {
        return addressId;
    }

    public void setAddressId(Long addressId) {
        this.addressId = addressId;
    }

    public Long getId() {
        return id;
    }

    public LocalDateTime getOrderDate() {
        return orderDate;
    }

    public BigDecimal getTotalAmount() { 
        return totalAmount;
    }

    public List<OrderItemDTO> getOrderItems() {
        return orderItems;
    }

    public Long getCustomerId() { 
        return customerId;
    }

   
    public void setId(Long id) {
        this.id = id;
    }
    public String getCustomerUsername() {
        return customerUsername;
    }

    public void setCustomerUsername(String customerUsername) {
        this.customerUsername = customerUsername;
    }


    public void setOrderDate(LocalDateTime orderDate) {
        this.orderDate = orderDate;
    }

    public void setTotalAmount(BigDecimal totalAmount) { 
        this.totalAmount = totalAmount;
    }

    public void setOrderItems(List<OrderItemDTO> orderItems) {
        this.orderItems = orderItems;
    }

    public void setCustomerId(Long customerId) { 
        this.customerId = customerId;
    }

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

    public BigDecimal getDiscountAmount() { 
        return discountAmount;
    }

    public void setDiscountAmount(BigDecimal discountAmount) { 
        this.discountAmount = discountAmount;
    }

    public BigDecimal getDiscountedAmount() { 
        return discountedAmount;
    }

    public void setDiscountedAmount(BigDecimal discountedAmount) { 
        this.discountedAmount = discountedAmount;
    }
}
