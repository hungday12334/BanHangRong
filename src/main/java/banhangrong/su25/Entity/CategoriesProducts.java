package banhangrong.su25.Entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "categories_products")
public class CategoriesProducts {

    @EmbeddedId
    private CategoriesProductsId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("categoryId")
    @JoinColumn(name = "category_id")
    private Categories category;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("productId")
    @JoinColumn(name = "product_id")
    private Products product;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    // Constructors
    public CategoriesProducts() {}

    public CategoriesProducts(Categories category, Products product) {
        this.category = category;
        this.product = product;
        this.id = new CategoriesProductsId(category.getCategoryId(), product.getProductId());
        this.createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public CategoriesProductsId getId() {
        return id;
    }

    public void setId(CategoriesProductsId id) {
        this.id = id;
    }

    public Categories getCategory() {
        return category;
    }

    public void setCategory(Categories category) {
        this.category = category;
    }

    public Products getProduct() {
        return product;
    }

    public void setProduct(Products product) {
        this.product = product;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

