package com.acc.dto;
public class AddItemToCartRequestDTO {
    private Long productId; 
    private Integer quantity;
	public AddItemToCartRequestDTO() {
		super();
		
	}
	public Long getProductId() {
		return productId;
	}
	public void setProductId(Long productId) {
		this.productId = productId;
	}
	public Integer getQuantity() {
		return quantity;
	}
	public void setQuantity(Integer quantity) {
		this.quantity = quantity;
	}  
}