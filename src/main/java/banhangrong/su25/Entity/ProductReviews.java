package banhangrong.su25.Entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "product_reviews")
public class ProductReviews {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "review_id")
    private Long reviewId;

    @Column(name = "product_id")
    private Long productId;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "order_item_id")
    private Long orderItemId; // new: link to order item (nullable)

    private Integer rating;
    private String comment;

    // THÊM 2 FIELD MỚI
    @Column(name = "seller_response", columnDefinition = "TEXT")
    private String sellerResponse;

    @Column(name = "seller_response_at")
    private LocalDateTime sellerResponseAt;

    @Column(name = "media_urls", columnDefinition = "TEXT")
    private String mediaUrls; // new: comma separated media URLs

    @Column(name = "service_rating")
    private Integer serviceRating; // new: optional service rating

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Thêm getters và setters cho 2 field mới
    public String getSellerResponse() {
        return sellerResponse;
    }

    public void setSellerResponse(String sellerResponse) {
        this.sellerResponse = sellerResponse;
        this.sellerResponseAt = LocalDateTime.now();
    }

    public LocalDateTime getSellerResponseAt() {
        return sellerResponseAt;
    }

    public void setSellerResponseAt(LocalDateTime sellerResponseAt) {
        this.sellerResponseAt = sellerResponseAt;
    }

    // New getters/setters
    public Long getOrderItemId() {
        return orderItemId;
    }

    public void setOrderItemId(Long orderItemId) {
        this.orderItemId = orderItemId;
    }

    public String getMediaUrls() {
        return mediaUrls;
    }

    public void setMediaUrls(String mediaUrls) {
        this.mediaUrls = mediaUrls;
    }

    public Integer getServiceRating() {
        return serviceRating;
    }

    public void setServiceRating(Integer serviceRating) {
        this.serviceRating = serviceRating;
    }

    // Các getters và setters cũ giữ nguyên
    public Long getReviewId() {
        return reviewId;
    }
    public void setReviewId(Long reviewId) {
        this.reviewId = reviewId;
    }
    public Long getProductId() {
        return productId;
    }
    public void setProductId(Long productId) {
        this.productId = productId;
    }
    public Long getUserId() {
        return userId;
    }
    public void setUserId(Long userId) {
        this.userId = userId;
    }
    public Integer getRating() {
        return rating;
    }
    public void setRating(Integer rating) {
        this.rating = rating;
    }
    public String getComment() {
        return comment;
    }
    public void setComment(String comment) {
        this.comment = comment;
    }
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}