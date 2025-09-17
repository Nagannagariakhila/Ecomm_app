package com.acc.entity;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonBackReference;    // For Customer <-> Profile relationship
import com.fasterxml.jackson.annotation.JsonManagedReference; // For Profile <-> Address relationship

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
    @JsonBackReference 
    private Customer customer;

    
    @OneToMany(mappedBy = "profile", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference 
    private List<Address> addresses = new ArrayList<>();

    public Profile() {}

    public Profile(String firstName, String lastName, String phoneNumber, String email) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.phoneNumber = phoneNumber;
        this.email = email;
    }

   

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public Customer getCustomer() { return customer; }

    
    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    public List<Address> getAddresses() { return addresses; }

    public void setAddresses(List<Address> addresses) {
       
        if (this.addresses != null) {
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Profile profile = (Profile) o;
        return Objects.equals(id, profile.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Profile{" +
               "id=" + id +
               ", firstName='" + firstName + '\'' +
               ", lastName='" + lastName + '\'' +
               ", phoneNumber='" + phoneNumber + '\'' +
               ", email='" + email + '\'' +
              
               '}';
    }
}