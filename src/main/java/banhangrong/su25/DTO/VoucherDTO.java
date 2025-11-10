package banhangrong.su25.DTO;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Data Transfer Object for Voucher creation and update
 * Contains comprehensive validation rules for e-commerce voucher system
 */
public class VoucherDTO {

    // ============================================================
    // FIELDS
    // ============================================================

    private Long voucherId;

    @NotNull(message = "Seller ID không được để trống")
    private Long sellerId;

    @NotNull(message = "Product ID không được để trống")
    private Long productId;

    /**
     * Rule E: Voucher code validation
     * - Required
     * - 3-20 characters
     * - Must be unique per seller (checked in service layer)
     */
    @NotBlank(message = "Mã voucher không được để trống")
    @Size(min = 3, max = 20, message = "Mã voucher phải từ 3-20 ký tự")
    @Pattern(regexp = "^[A-Z0-9]+$", message = "Mã voucher chỉ được chứa chữ IN HOA và số")
    private String code;

    /**
     * Rule E: Discount type validation
     * - Required
     * - Must be "PERCENT" or "AMOUNT"
     */
    @NotBlank(message = "Loại giảm giá không được để trống")
    @Pattern(regexp = "^(PERCENT|AMOUNT)$", message = "Loại giảm giá phải là PERCENT hoặc AMOUNT")
    private String discountType;

    /**
     * Rule B (4, 5): Discount value validation
     * - Required
     * - Must be > 0
     * - If PERCENT: must be <= 100
     * - If AMOUNT: must be <= product price or minOrderValue
     */
    @NotNull(message = "Giá trị giảm giá không được để trống")
    @DecimalMin(value = "0.01", message = "Giá trị giảm giá phải lớn hơn 0")
    private BigDecimal discountValue;

    /**
     * Rule C (7, 8): Minimum order value validation
     * - Must be >= 0
     * - Must be <= 10,000,000 VND
     */
    @DecimalMin(value = "0", message = "Giá trị đơn hàng tối thiểu phải >= 0")
    @DecimalMax(value = "10000000", message = "Giá trị đơn hàng tối thiểu không được vượt quá 10,000,000 VNĐ")
    private BigDecimal minOrder;

    /**
     * Rule A (1, 2): Start date validation
     * - Required
     * - Must not be in the past
     * - Must be before endDate
     */
    @NotNull(message = "Ngày bắt đầu không được để trống")
    @Future(message = "Ngày bắt đầu phải là ngày trong tương lai")
    private LocalDateTime startAt;

    /**
     * Rule A (2, 3): End date validation
     * - Required
     * - Must be after startDate
     * - Duration should not exceed 1 year
     */
    @NotNull(message = "Ngày kết thúc không được để trống")
    @Future(message = "Ngày kết thúc phải là ngày trong tương lai")
    private LocalDateTime endAt;

    /**
     * Rule D (10): Max uses validation
     * - Must be >= 0 (0 means unlimited)
     */
    @Min(value = 1, message = "Số lần sử dụng tối đa phải > 0")
    private Integer maxUses;

    /**
     * Rule D (10, 11): Limit per user validation
     * - Must be >= 0 (0 means unlimited)
     * - Must be <= maxUses (if maxUses > 0)
     */
    @Min(value = 1, message = "Số lần sử dụng tối đa/người phải > 0")
    private Integer maxUsesPerUser;

    /**
     * Rule E: Status validation
     * - Required
     * - Must be "active" or "inactive"
     */
    @NotBlank(message = "Trạng thái không được để trống")
    @Pattern(regexp = "^(active|inactive)$", message = "Trạng thái phải là 'active' hoặc 'inactive'")
    private String status;

    // Product price for validation (not persisted)
    private BigDecimal productPrice;

    // ============================================================
    // COMPLEX VALIDATION RULES (Custom Validators)
    // ============================================================

    /**
     * Rule A.2: Validate that endDate is after startDate
     */
    @AssertTrue(message = "Ngày kết thúc phải sau ngày bắt đầu")
    public boolean isEndDateAfterStartDate() {
        if (startAt == null || endAt == null) {
            return true; // Let @NotNull handle this
        }
        return endAt.isAfter(startAt);
    }

    /**
     * Rule A.3: Validate that duration does not exceed 1 year
     */
    @AssertTrue(message = "Thời gian voucher không được vượt quá 1 năm")
    public boolean isDurationWithinOneYear() {
        if (startAt == null || endAt == null) {
            return true; // Let @NotNull handle this
        }
        LocalDateTime oneYearLater = startAt.plusYears(1);
        return !endAt.isAfter(oneYearLater);
    }

    /**
     * Rule B.4: If discountType is PERCENT, discountValue must be <= 100
     */
    @AssertTrue(message = "Giá trị giảm giá % phải từ 0.01 đến 100")
    public boolean isPercentageValueValid() {
        if (discountType == null || discountValue == null) {
            return true; // Let @NotNull handle this
        }
        if ("PERCENT".equalsIgnoreCase(discountType)) {
            return discountValue.compareTo(BigDecimal.ZERO) > 0
                && discountValue.compareTo(BigDecimal.valueOf(100)) <= 0;
        }
        return true;
    }

    /**
     * Rule B.5: If discountType is AMOUNT, discountValue must be <= product price
     */
    @AssertTrue(message = "Giá trị giảm giá không được vượt quá giá sản phẩm")
    public boolean isAmountValueValidAgainstProductPrice() {
        if (discountType == null || discountValue == null || productPrice == null) {
            return true; // Skip if not available
        }
        if ("AMOUNT".equalsIgnoreCase(discountType)) {
            return discountValue.compareTo(productPrice) <= 0;
        }
        return true;
    }

    /**
     * Rule B.6: discountValue should not exceed minOrderValue
     */
    @AssertTrue(message = "Giá trị giảm giá không được vượt quá giá trị đơn hàng tối thiểu")
    public boolean isDiscountValueNotExceedMinOrder() {
        if (discountValue == null || minOrder == null) {
            return true; // Skip if minOrder is not set
        }
        return discountValue.compareTo(minOrder) <= 0;
    }

    /**
     * Rule C.9: If discountType is AMOUNT, minOrderValue must be >= discountValue
     */
    @AssertTrue(message = "Giá trị đơn hàng tối thiểu phải >= giá trị giảm giá (với loại AMOUNT)")
    public boolean isMinOrderValidForAmountDiscount() {
        if (discountType == null || discountValue == null || minOrder == null) {
            return true; // Skip if not available
        }
        if ("AMOUNT".equalsIgnoreCase(discountType)) {
            return minOrder.compareTo(discountValue) >= 0;
        }
        return true;
    }

    /**
     * Rule D.11: limitPerUser must be <= maxUses (if maxUses > 0)
     */
    @AssertTrue(message = "Số lần sử dụng/người không được vượt quá tổng số lần sử dụng")
    public boolean isLimitPerUserValidAgainstMaxUses() {
        if (maxUses == null || maxUsesPerUser == null) {
            return true; // Skip if not set
        }
        // If maxUses > 0, limitPerUser must be <= maxUses
        if (maxUses > 0 && maxUsesPerUser > 0) {
            return maxUsesPerUser <= maxUses;
        }
        return true;
    }

    /**
     * Rule A.1: Validate that startDate is not in the past
     * Note: @Future annotation handles this, but we add manual check for better message
     */
    @AssertTrue(message = "Ngày bắt đầu không được là ngày trong quá khứ")
    public boolean isStartDateNotInPast() {
        if (startAt == null) {
            return true; // Let @NotNull handle this
        }
        // Allow start date to be today or future
        return !startAt.isBefore(LocalDateTime.now().minusMinutes(5)); // 5 min tolerance
    }

    // ============================================================
    // CONSTRUCTORS
    // ============================================================

    public VoucherDTO() {
    }

    public VoucherDTO(Long voucherId, Long sellerId, Long productId, String code,
                      String discountType, BigDecimal discountValue, BigDecimal minOrder,
                      LocalDateTime startAt, LocalDateTime endAt,
                      Integer maxUses, Integer maxUsesPerUser, String status) {
        this.voucherId = voucherId;
        this.sellerId = sellerId;
        this.productId = productId;
        this.code = code;
        this.discountType = discountType;
        this.discountValue = discountValue;
        this.minOrder = minOrder;
        this.startAt = startAt;
        this.endAt = endAt;
        this.maxUses = maxUses;
        this.maxUsesPerUser = maxUsesPerUser;
        this.status = status;
    }

    // ============================================================
    // GETTERS AND SETTERS
    // ============================================================

    public Long getVoucherId() {
        return voucherId;
    }

    public void setVoucherId(Long voucherId) {
        this.voucherId = voucherId;
    }

    public Long getSellerId() {
        return sellerId;
    }

    public void setSellerId(Long sellerId) {
        this.sellerId = sellerId;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code != null ? code.toUpperCase().trim() : null;
    }

    public String getDiscountType() {
        return discountType;
    }

    public void setDiscountType(String discountType) {
        this.discountType = discountType;
    }

    public BigDecimal getDiscountValue() {
        return discountValue;
    }

    public void setDiscountValue(BigDecimal discountValue) {
        this.discountValue = discountValue;
    }

    public BigDecimal getMinOrder() {
        return minOrder;
    }

    public void setMinOrder(BigDecimal minOrder) {
        this.minOrder = minOrder;
    }

    public LocalDateTime getStartAt() {
        return startAt;
    }

    public void setStartAt(LocalDateTime startAt) {
        this.startAt = startAt;
    }

    public LocalDateTime getEndAt() {
        return endAt;
    }

    public void setEndAt(LocalDateTime endAt) {
        this.endAt = endAt;
    }

    public Integer getMaxUses() {
        return maxUses;
    }

    public void setMaxUses(Integer maxUses) {
        this.maxUses = maxUses;
    }

    public Integer getMaxUsesPerUser() {
        return maxUsesPerUser;
    }

    public void setMaxUsesPerUser(Integer maxUsesPerUser) {
        this.maxUsesPerUser = maxUsesPerUser;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public BigDecimal getProductPrice() {
        return productPrice;
    }

    public void setProductPrice(BigDecimal productPrice) {
        this.productPrice = productPrice;
    }

    // ============================================================
    // UTILITY METHODS
    // ============================================================

    @Override
    public String toString() {
        return "VoucherDTO{" +
                "voucherId=" + voucherId +
                ", sellerId=" + sellerId +
                ", productId=" + productId +
                ", code='" + code + '\'' +
                ", discountType='" + discountType + '\'' +
                ", discountValue=" + discountValue +
                ", minOrder=" + minOrder +
                ", startAt=" + startAt +
                ", endAt=" + endAt +
                ", maxUses=" + maxUses +
                ", maxUsesPerUser=" + maxUsesPerUser +
                ", status='" + status + '\'' +
                '}';
    }
}

