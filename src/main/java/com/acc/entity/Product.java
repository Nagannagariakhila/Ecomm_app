package com.acc.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.*;

@Entity
@Table(name = "products")
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(length = 1000)
    private String description;

    @Column(name = "image_urls")
    private String imageUrls;

    @Column(nullable = false)
    private BigDecimal price;

    @Column(nullable = false)
    private Integer stockQuantity;

    @Column(nullable = false)
    private Category category;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "discount_percentage")
    private Double discountPercentage = 0.0;

    public Product() {}

    public Product(Long id, String name, String description, String imageUrls, BigDecimal price, Integer stockQuantity, Category category) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.imageUrls = imageUrls;
        this.price = price;
        this.stockQuantity = stockQuantity;
        this.category = category;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public Integer getStockQuantity() { return stockQuantity; }
    public void setStockQuantity(Integer stockQuantity) { this.stockQuantity = stockQuantity; }
    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public String getRawImageUrls() { return imageUrls; }
    public void setRawImageUrls(String imageUrls) { this.imageUrls = imageUrls; }

    public List<String> getImageUrlsList() {
        if (this.imageUrls == null || this.imageUrls.trim().isEmpty()) {
            return Collections.emptyList();
        }
        return Arrays.asList(this.imageUrls.split(","));
    }

    public void setImageUrlsList(List<String> images) {
        this.imageUrls = (images != null && !images.isEmpty()) ? String.join(",", images) : null;
    }

    public Double getDiscountPercentage() {
        return discountPercentage;
    }

    public void setDiscountPercentage(Double discountPercentage) {
        this.discountPercentage = discountPercentage;
    }

    public BigDecimal getDiscountedPrice() {
        if (discountPercentage == null || discountPercentage <= 0) return price;
        BigDecimal discount = price.multiply(BigDecimal.valueOf(discountPercentage / 100));
        return price.subtract(discount);
    }

	public void setCategory(String trim) {
		
		
	}

	public Object getProductId() {
		
		return null;
	}

	public void setAverageRating(double averageRating) {
		// TODO Auto-generated method stub
		
	}

	
}