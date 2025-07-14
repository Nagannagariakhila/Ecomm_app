package com.acc.entity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
@Entity
@Table(name = "payments") 
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; 
    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "order_id", nullable = true) 
    private Order order;
    @Column(nullable = false)
    private LocalDateTime paymentDate; 
    @Column(nullable = false)
    private BigDecimal amount; 
    @Column(name = "payment_method", nullable = false)
    private String paymentMethod; 
    @Column(nullable = false)
    private String status; 
    private String customerUsername; 


    public Payment() {
        
    }

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Order getOrder() {
		return order;
	}

	public void setOrder(Order order) {
		this.order = order;
	}

	public LocalDateTime getPaymentDate() {
		return paymentDate;
	}

	public void setPaymentDate(LocalDateTime paymentDate) {
		this.paymentDate = paymentDate;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public void setAmount(BigDecimal amount) {
		this.amount = amount;
	}

	public String getPaymentMethod() {
		return paymentMethod;
	}

	public void setPaymentMethod(String paymentMethod) {
		this.paymentMethod = paymentMethod;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	 public String getCustomerUsername() {
	        return customerUsername;
	    }

	    public void setCustomerUsername(String customerUsername) {
	        this.customerUsername = customerUsername;
	    }
    

   }