package com.acc.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.DiscriminatorValue; // Import DiscriminatorValue

@Entity
@Table(name = "admins") 
@PrimaryKeyJoinColumn(name = "id")
@DiscriminatorValue("Admin") 
public class Admin extends User {

    public Admin() {
        super();
    }   
}
