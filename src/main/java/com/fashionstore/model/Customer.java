package com.fashionstore.model;

import jakarta.persistence.*;

@Entity
@Table(name = "customers")
public class Customer {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable=false, unique=true)
    private String email;
    @Column(nullable=false)
    private String fullName;
    @Column(nullable=false)
    private String passwordHash;

    public Customer() {}
    public Customer(String fullName, String email, String passwordHash) {
        this.fullName=fullName; this.email=email; this.passwordHash=passwordHash;
    }
    public Long getId(){return id;}
    public String getEmail(){return email;}
    public String getFullName(){return fullName;}
    public String getPasswordHash(){return passwordHash;}
}
