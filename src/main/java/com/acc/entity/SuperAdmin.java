package com.acc.entity;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;

@Entity
@Table(name = "super_admins")
@PrimaryKeyJoinColumn(name = "id")
@DiscriminatorValue("SUPER_ADMIN")
public class SuperAdmin extends User {
    public SuperAdmin() {
        super();
    }
}


