package banhangrong.su25.Entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "product_licenses")
public class ProductLicenses {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "license_id")
    private Long licenseId;
    @Column(name = "order_item_id")
    private Long orderItemId;
    @Column(name = "user_id")
    private Long userId;
    @Column(name = "license_key")
    private String licenseKey;
    @Column(name = "is_active")
    private Boolean isActive;
    @Column(name = "activation_date")
    private LocalDateTime activationDate;
    @Column(name = "last_used_date")
    private LocalDateTime lastUsedDate;
    @Column(name = "device_identifier")
    private String deviceIdentifier;
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public Long getLicenseId() {
        return licenseId;
    }
    public void setLicenseId(Long licenseId) {
        this.licenseId = licenseId;
    }
    public Long getOrderItemId() {
        return orderItemId;
    }
    public void setOrderItemId(Long orderItemId) {
        this.orderItemId = orderItemId;
    }
    public Long getUserId() {
        return userId;
    }
    public void setUserId(Long userId) {
        this.userId = userId;
    }
    public String getLicenseKey() {
        return licenseKey;
    }
    public void setLicenseKey(String licenseKey) {
        this.licenseKey = licenseKey;
    }
    public Boolean getIsActive() {
        return isActive;
    }
    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
    public LocalDateTime getActivationDate() {
        return activationDate;
    }
    public void setActivationDate(LocalDateTime activationDate) {
        this.activationDate = activationDate;
    }
    public LocalDateTime getLastUsedDate() {
        return lastUsedDate;
    }
    public void setLastUsedDate(LocalDateTime lastUsedDate) {
        this.lastUsedDate = lastUsedDate;
    }
    public String getDeviceIdentifier() {
        return deviceIdentifier;
    }
    public void setDeviceIdentifier(String deviceIdentifier) {
        this.deviceIdentifier = deviceIdentifier;
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
}
