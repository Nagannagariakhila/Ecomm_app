package com.acc.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "customer")
@PrimaryKeyJoinColumn(name = "id")
public class Customer extends User {

    @Column(name = "customer_code", nullable = true, unique = true)
    private String customerCode;

  
    @OneToOne(mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true)
    private Profile profile;

    public Customer() {
    }

   

    public String getCustomerCode() {
        return customerCode;
    }

    public void setCustomerCode(String customerCode) {
        this.customerCode = customerCode;
    }

    public Profile getProfile() {
        return profile;
    }

   
    public void setProfile(Profile profile) {
        if (this.profile == profile) {
            return;
        }

        if (this.profile != null) {
            this.profile.setCustomer(null);
        }

        this.profile = profile;

        if (profile != null) {
            profile.setCustomer(this);
        }
    }
}