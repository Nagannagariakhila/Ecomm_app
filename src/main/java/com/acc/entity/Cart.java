package com.acc.entity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects; 
@Entity
@Table(name = "carts")
public class Cart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false, unique = true)
    private Customer customer;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column(nullable = false) 
    private BigDecimal totalAmount;

    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CartItem> cartItems = new ArrayList<>();
    public Cart() {
        
    }

    public Cart(Long id, Customer customer, LocalDateTime createdAt, LocalDateTime updatedAt, BigDecimal totalAmount, List<CartItem> cartItems) {
        this.id = id;
        this.customer = customer;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.totalAmount = totalAmount;
        if (cartItems != null) {
            this.cartItems = new ArrayList<>(cartItems);
            this.cartItems.forEach(item -> item.setCart(this));
        } else {
            this.cartItems = new ArrayList<>();
        }
    }

    public Long getId() {
        return id;
    }

    public Customer getCustomer() {
        return customer;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public List<CartItem> getCartItems() {
        return cartItems;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }
    public void setCartItems(List<CartItem> cartItems) {
        this.cartItems.clear();
        if (cartItems != null) {
            for (CartItem item : cartItems) {
                this.addCartItem(item); 
            }
        }
    }
    public void addCartItem(CartItem cartItem) {
        if (cartItems == null) {
            cartItems = new ArrayList<>();
        }
        cartItems.add(cartItem);
        cartItem.setCart(this);
    }

    public void removeCartItem(CartItem cartItem) {
        if (cartItems != null) {
            cartItems.remove(cartItem);
            cartItem.setCart(null);
        }
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (totalAmount == null) {
            totalAmount = BigDecimal.ZERO;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Cart cart = (Cart) o;
        return Objects.equals(id, cart.id); 
    }

    @Override
    public int hashCode() {
        return Objects.hash(id); 
    }

    
    @Override
    public String toString() {
        return "Cart{" +
               "id=" + id +
               ", customerId=" + (customer != null ? customer.getId() : "null") + 
               ", createdAt=" + createdAt +
               ", updatedAt=" + updatedAt +
               ", totalAmount=" + totalAmount +
               
               '}';
    }

	public void setCartItems(Object cartItems2) {
		
	}
}