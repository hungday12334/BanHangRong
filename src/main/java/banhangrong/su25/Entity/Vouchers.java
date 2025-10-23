package banhangrong.su25.Entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "vouchers")
public class Vouchers {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "voucher_id")
    private Long voucherId;

    @Column(name = "seller_id", nullable = false)
    private Long sellerId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "code", nullable = false, length = 64)
    private String code;

    // percent or amount
    @Column(name = "discount_type", nullable = false, length = 16)
    private String discountType; // PERCENT | AMOUNT

    @Column(name = "discount_value", nullable = false, precision = 12, scale = 2)
    private BigDecimal discountValue;

    @Column(name = "min_order", precision = 12, scale = 2)
    private BigDecimal minOrder;

    @Column(name = "start_at")
    private LocalDateTime startAt;

    @Column(name = "end_at")
    private LocalDateTime endAt;

    @Column(name = "max_uses")
    private Integer maxUses; // null = unlimited

    @Column(name = "max_uses_per_user")
    private Integer maxUsesPerUser; // null = unlimited

    @Column(name = "used_count")
    private Integer usedCount;

    @Column(name = "status", length = 16)
    private String status; // active | inactive | expired

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void _prePersist() {
        if (usedCount == null) usedCount = 0;
        if (status == null || status.isBlank()) status = "active";
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
        if (discountType != null) discountType = discountType.toUpperCase();
    }

    @PreUpdate
    public void _preUpdate() {
        updatedAt = LocalDateTime.now();
        if (status != null) status = status.toLowerCase();
        if (discountType != null) discountType = discountType.toUpperCase();
    }

    public Long getVoucherId() { return voucherId; }
    public void setVoucherId(Long voucherId) { this.voucherId = voucherId; }
    public Long getSellerId() { return sellerId; }
    public void setSellerId(Long sellerId) { this.sellerId = sellerId; }
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getDiscountType() { return discountType; }
    public void setDiscountType(String discountType) { this.discountType = discountType; }
    public BigDecimal getDiscountValue() { return discountValue; }
    public void setDiscountValue(BigDecimal discountValue) { this.discountValue = discountValue; }
    public BigDecimal getMinOrder() { return minOrder; }
    public void setMinOrder(BigDecimal minOrder) { this.minOrder = minOrder; }
    public LocalDateTime getStartAt() { return startAt; }
    public void setStartAt(LocalDateTime startAt) { this.startAt = startAt; }
    public LocalDateTime getEndAt() { return endAt; }
    public void setEndAt(LocalDateTime endAt) { this.endAt = endAt; }
    public Integer getMaxUses() { return maxUses; }
    public void setMaxUses(Integer maxUses) { this.maxUses = maxUses; }
    public Integer getMaxUsesPerUser() { return maxUsesPerUser; }
    public void setMaxUsesPerUser(Integer maxUsesPerUser) { this.maxUsesPerUser = maxUsesPerUser; }
    public Integer getUsedCount() { return usedCount; }
    public void setUsedCount(Integer usedCount) { this.usedCount = usedCount; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
