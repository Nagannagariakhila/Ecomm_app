package com.acc.entity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime; 
import java.util.ArrayList;
import java.util.List;
@Entity
@Table(name = "customer_order") 
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; 
    private LocalDateTime orderDate;
    private BigDecimal totalAmount;
    private String status; 
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id") 
    private Customer customer;
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> orderItems = new ArrayList<>();
    @ManyToOne
    @JoinColumn(name = "address_id")
    private Address shippingAddress;
    public Order() {}

    public Address getShippingAddress() {
        return shippingAddress;
    }

    public void setShippingAddress(Address shippingAddress) {
        this.shippingAddress = shippingAddress;
    }
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public LocalDateTime getOrderDate() { return orderDate; }
    public void setOrderDate(LocalDateTime orderDate) { this.orderDate = orderDate; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Customer getCustomer() { return customer; }
    public void setCustomer(Customer customer) { this.customer = customer; }
    public List<OrderItem> getOrderItems() { return orderItems; }
    public void setOrderItems(List<OrderItem> orderItems) { this.orderItems = orderItems; }

    
    public void addOrderItem(OrderItem item) {
        if (this.orderItems == null) this.orderItems = new ArrayList<>();
        this.orderItems.add(item);
        item.setOrder(this);
    }

    public void removeOrderItem(OrderItem item) {
        if (this.orderItems != null) {
            this.orderItems.remove(item);
            item.setOrder(null);
        }
    }

	
}