// Owner: Mehwish | Database & Config | JPA entity for products in the e-Kiosk catalogue
package com.shopping.system.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    // Low-stock alert threshold used in admin dashboard is < 5
    @Column(name = "quantity_on_hand", nullable = false)
    private int quantityOnHand;

    @Column(length = 50)
    private String category;

    @Column(name = "image_url", length = 255)
    private String imageUrl;

    public Product() {}

    public Product(String name, String description, BigDecimal price,
                   int quantityOnHand, String category) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.quantityOnHand = quantityOnHand;
        this.category = category;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public int getQuantityOnHand() { return quantityOnHand; }
    public void setQuantityOnHand(int quantityOnHand) { this.quantityOnHand = quantityOnHand; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
}
