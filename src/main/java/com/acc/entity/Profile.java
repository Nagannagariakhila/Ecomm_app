package com.acc.entity;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "profiles")
public class Profile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String firstName;
    private String lastName;
    private String phoneNumber;
    
    @Column(unique = true)
    
    private String email;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", unique = true, nullable = false)
    private Customer customer;

    @OneToMany(mappedBy = "profile", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Address> addresses = new ArrayList<>();

    public Profile() {}

    public Profile(String firstName, String lastName, String phoneNumber,String email) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.phoneNumber = phoneNumber;
        this.email = email;
    }

    
    public Long getId() { return id; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getPhoneNumber() { return phoneNumber; }
    public String getEmail() { return email; } 
    public Customer getCustomer() { return customer; }
    public List<Address> getAddresses() { return addresses; }

    
    public void setId(Long id) { this.id = id; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    public void setEmail(String email) { this.email = email; }  

    public void setCustomer(Customer customer) {
        if (this.customer == customer) return;

        if (this.customer != null) {
            if (this.customer.getProfile() != null && this.customer.getProfile().equals(this)) {
                this.customer.setProfile(null);
            }
        }

        this.customer = customer;

        if (customer != null && (customer.getProfile() == null || !customer.getProfile().equals(this))) {
            customer.setProfile(this);
        }
    }

    public void setAddresses(List<Address> addresses) {
        if (this.addresses != null) {
            this.addresses.forEach(address -> address.setProfile(null));
            this.addresses.clear();
        } else {
            this.addresses = new ArrayList<>();
        }

        if (addresses != null) {
            for (Address address : addresses) {
                addAddress(address);
            }
        }
    }

    public void addAddress(Address address) {
        if (address != null && !this.addresses.contains(address)) {
            this.addresses.add(address);
            address.setProfile(this);
        }
    }

    public void removeAddress(Address address) {
        if (address != null && this.addresses.contains(address)) {
            this.addresses.remove(address);
            address.setProfile(null);
        }
    }
}
