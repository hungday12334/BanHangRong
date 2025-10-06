package banhangrong.su25.Entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "product_images")
public class ProductImages {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "image_id")
    private Long imageId;
    @Column(name = "product_id")
    private Long productId;
    @Column(name = "image_url")
    private String imageUrl;
    @Column(name = "is_primary")
    private Boolean isPrimary;
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public Long getImageId() {
        return imageId;
    }
    public void setImageId(Long imageId) {
        this.imageId = imageId;
    }
    public Long getProductId() {
        return productId;
    }
    public void setProductId(Long productId) {
        this.productId = productId;
    }
    public String getImageUrl() {
        return imageUrl;
    }
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
    public Boolean getIsPrimary() {
        return isPrimary;
    }
    public void setIsPrimary(Boolean isPrimary) {
        this.isPrimary = isPrimary;
    }
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
