package com.example.productapp.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

@Entity
@Table(name = "products")
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @NotBlank
    private String name;
    @PositiveOrZero
    private Double price;

    public Product() {}
    public Product(String name, Double price) { this.name = name; this.price = price; }
    public Long getId() { return id; }
    public String getName() { return name; }
    public Double getPrice() { return price; }
    public void setId(Long id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setPrice(Double price) { this.price = price; }
}
