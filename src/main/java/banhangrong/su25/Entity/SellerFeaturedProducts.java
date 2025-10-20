// SellerFeaturedProducts.java - CẦN SỬA LẠI
package banhangrong.su25.Entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "seller_featured_products")
public class SellerFeaturedProducts {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "featured_id")
    private Long featuredId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "section_id", nullable = false)
    private SellerShopSections section;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Products product;

    @Column(name = "sort_order")
    private Integer sortOrder = 0;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    // Constructors
    public SellerFeaturedProducts() {
        this.createdAt = LocalDateTime.now();
    }

    public SellerFeaturedProducts(SellerShopSections section, Products product, Integer sortOrder) {
        this();
        this.section = section;
        this.product = product;
        this.sortOrder = sortOrder;
    }

    // Getters and Setters
    public Long getFeaturedId() { return featuredId; }
    public void setFeaturedId(Long featuredId) { this.featuredId = featuredId; }

    public SellerShopSections getSection() { return section; }
    public void setSection(SellerShopSections section) { this.section = section; }

    public Products getProduct() { return product; }
    public void setProduct(Products product) { this.product = product; }

    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
