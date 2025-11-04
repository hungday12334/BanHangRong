package banhangrong.su25.Entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity to track which licenses are assigned to each category
 * This allows sellers to manage licenses per category without deleting them from database
 */
@Entity
@Table(name = "category_licenses")
public class CategoryLicenses {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "category_license_id")
    private Long categoryLicenseId;

    @Column(name = "category_id", nullable = false)
    private Long categoryId;

    @Column(name = "shop_license_id", nullable = false)
    private Long shopLicenseId;

    @Column(name = "seller_id", nullable = false)
    private Long sellerId;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getCategoryLicenseId() {
        return categoryLicenseId;
    }

    public void setCategoryLicenseId(Long categoryLicenseId) {
        this.categoryLicenseId = categoryLicenseId;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public Long getShopLicenseId() {
        return shopLicenseId;
    }

    public void setShopLicenseId(Long shopLicenseId) {
        this.shopLicenseId = shopLicenseId;
    }

    public Long getSellerId() {
        return sellerId;
    }

    public void setSellerId(Long sellerId) {
        this.sellerId = sellerId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

