package com.fashionstore.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "store_orders")
public class StoreOrder {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long customerId;
    private String customerName;
    private String customerEmail;
    @Column(length=2000)
    private String itemsSummary;
    @Column(precision=10, scale=2)
    private BigDecimal total;
    private String status;
    private LocalDateTime createdAt;

    public StoreOrder() {}
    public StoreOrder(Long customerId, String customerName, String customerEmail, String itemsSummary, BigDecimal total) {
        this.customerId=customerId; this.customerName=customerName; this.customerEmail=customerEmail;
        this.itemsSummary=itemsSummary; this.total=total; this.status="Placed"; this.createdAt=LocalDateTime.now();
    }
    public Long getId(){return id;}
    public Long getCustomerId(){return customerId;}
    public String getCustomerName(){return customerName;}
    public String getCustomerEmail(){return customerEmail;}
    public String getItemsSummary(){return itemsSummary;}
    public BigDecimal getTotal(){return total;}
    public String getStatus(){return status;}
    public LocalDateTime getCreatedAt(){return createdAt;}
}
