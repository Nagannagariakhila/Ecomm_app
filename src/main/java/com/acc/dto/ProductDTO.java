package com.acc.dto;
import java.math.BigDecimal;
import java.util.List;
import com.acc.entity.Category; 

public class ProductDTO {
    private Long id;
    private String name;
    private String description;
    private List<String> images;
    private BigDecimal price;
    private Integer stockQuantity;
    private Category category; 

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public List<String> getImages() { return images; }
    public void setImages(List<String> images) { this.images = images; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public Integer getStockQuantity() { return stockQuantity; }
    public void setStockQuantity(Integer stockQuantity) { this.stockQuantity = stockQuantity; }

    public Category getCategory() { 
        return category;
    }

    public void setCategory(Category category) { 
        this.category = category;
    }
}