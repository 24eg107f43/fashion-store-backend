package com.fashionstore.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "products")
public class Product {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    @Column(length = 600)
    private String description;
    @Column(precision = 10, scale = 2)
    private BigDecimal price;
    private int stock;
    private String category;

    public Product() {}
    public Product(String name, String description, BigDecimal price, int stock, String category) {
        this.name=name; this.description=description; this.price=price; this.stock=stock; this.category=category;
    }
    public Long getId(){return id;}
    public String getName(){return name;}
    public String getDescription(){return description;}
    public BigDecimal getPrice(){return price;}
    public int getStock(){return stock;}
    public String getCategory(){return category;}
    public void setStock(int stock){this.stock=stock;}
}
