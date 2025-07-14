package com.acc.dto;
import java.math.BigDecimal;
import com.acc.entity.Category; 
public class OrderItemDTO {
    private Long id;
    private ProductDTO productDetails;
    private int quantity;
    private BigDecimal price; 
    private Long orderId;     

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ProductDTO getProductDetails() {
        return productDetails;
    }

    public void setProductDetails(ProductDTO productDetails) {
        this.productDetails = productDetails;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }
}