package banhangrong.su25.service;

import banhangrong.su25.Entity.VoucherRedemptions;
import banhangrong.su25.Entity.Vouchers;
import banhangrong.su25.Repository.VoucherRedemptionsRepository;
import banhangrong.su25.Repository.VouchersRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Professional Voucher Service for E-commerce Platform
 * Handles all business logic for voucher management and redemption
 */
@Service
@Transactional
public class VoucherService {

    private final VouchersRepository vouchersRepository;
    private final VoucherRedemptionsRepository redemptionsRepository;

    public VoucherService(VouchersRepository vouchersRepository,
                         VoucherRedemptionsRepository redemptionsRepository) {
        this.vouchersRepository = vouchersRepository;
        this.redemptionsRepository = redemptionsRepository;
    }

    /**
     * Validation result for voucher applicability
     */
    public static class VoucherValidationResult {
        private boolean valid;
        private String errorCode;
        private String errorMessage;
        private BigDecimal discountAmount;
        private Vouchers voucher;

        public static VoucherValidationResult success(Vouchers voucher, BigDecimal discountAmount) {
            VoucherValidationResult result = new VoucherValidationResult();
            result.valid = true;
            result.voucher = voucher;
            result.discountAmount = discountAmount;
            return result;
        }

        public static VoucherValidationResult error(String code, String message) {
            VoucherValidationResult result = new VoucherValidationResult();
            result.valid = false;
            result.errorCode = code;
            result.errorMessage = message;
            return result;
        }

        // Getters
        public boolean isValid() { return valid; }
        public String getErrorCode() { return errorCode; }
        public String getErrorMessage() { return errorMessage; }
        public BigDecimal getDiscountAmount() { return discountAmount; }
        public Vouchers getVoucher() { return voucher; }
    }

    /**
     * Validate and calculate discount for a voucher
     * @param voucherCode The voucher code to validate
     * @param userId The user attempting to use the voucher
     * @param productId The product the voucher is being applied to
     * @param orderAmount The order amount before discount
     * @return Validation result with discount amount if valid
     */
    public VoucherValidationResult validateAndCalculateDiscount(String voucherCode, Long userId,
                                                                Long productId, BigDecimal orderAmount) {
        // Normalize code
        if (voucherCode == null || voucherCode.trim().isEmpty()) {
            return VoucherValidationResult.error("INVALID_CODE", "Mã voucher không hợp lệ");
        }
        voucherCode = voucherCode.trim().toUpperCase();

        // Find voucher
        List<Vouchers> vouchers = vouchersRepository.findByCodeIgnoreCaseOrderByUpdatedAtDesc(voucherCode);
        if (vouchers.isEmpty()) {
            return VoucherValidationResult.error("NOT_FOUND", "Không tìm thấy mã voucher này");
        }

        // Get the most recent voucher with this code
        Vouchers voucher = vouchers.get(0);

        // Check if voucher belongs to the product
        if (!voucher.getProductId().equals(productId)) {
            return VoucherValidationResult.error("WRONG_PRODUCT", "Voucher không áp dụng cho sản phẩm này");
        }

        // Check status
        if (!"active".equalsIgnoreCase(voucher.getStatus())) {
            return VoucherValidationResult.error("INACTIVE", "Mã voucher không còn hiệu lực");
        }

        LocalDateTime now = LocalDateTime.now();

        // Check start date
        if (voucher.getStartAt() != null && now.isBefore(voucher.getStartAt())) {
            return VoucherValidationResult.error("NOT_STARTED", "Mã voucher chưa có hiệu lực");
        }

        // Check end date
        if (voucher.getEndAt() != null && now.isAfter(voucher.getEndAt())) {
            return VoucherValidationResult.error("EXPIRED", "Mã voucher đã hết hạn");
        }

        // Check minimum order amount
        if (voucher.getMinOrder() != null && orderAmount.compareTo(voucher.getMinOrder()) < 0) {
            return VoucherValidationResult.error("MIN_ORDER_NOT_MET",
                String.format("Đơn hàng tối thiểu %s để sử dụng voucher này", formatCurrency(voucher.getMinOrder())));
        }

        // Check max uses
        if (voucher.getMaxUses() != null) {
            int usedCount = voucher.getUsedCount() != null ? voucher.getUsedCount() : 0;
            if (usedCount >= voucher.getMaxUses()) {
                return VoucherValidationResult.error("MAX_USES_REACHED", "Voucher đã hết lượt sử dụng");
            }
        }

        // Check max uses per user
        if (voucher.getMaxUsesPerUser() != null && userId != null) {
            long userUsageCount = redemptionsRepository.countByVoucherIdAndUserId(voucher.getVoucherId(), userId);
            if (userUsageCount >= voucher.getMaxUsesPerUser()) {
                return VoucherValidationResult.error("USER_LIMIT_REACHED", "Bạn đã sử dụng hết lượt cho voucher này");
            }
        }

        // Calculate discount
        BigDecimal discountAmount = calculateDiscountAmount(voucher, orderAmount);

        return VoucherValidationResult.success(voucher, discountAmount);
    }

    /**
     * Calculate the discount amount based on voucher type
     */
    private BigDecimal calculateDiscountAmount(Vouchers voucher, BigDecimal orderAmount) {
        if ("PERCENT".equalsIgnoreCase(voucher.getDiscountType())) {
            // Percentage discount
            BigDecimal percentage = voucher.getDiscountValue();
            return orderAmount.multiply(percentage).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        } else if ("AMOUNT".equalsIgnoreCase(voucher.getDiscountType())) {
            // Fixed amount discount (but not more than order amount)
            BigDecimal fixedDiscount = voucher.getDiscountValue();
            return orderAmount.compareTo(fixedDiscount) < 0 ? orderAmount : fixedDiscount;
        }
        return BigDecimal.ZERO;
    }

    /**
     * Redeem a voucher (should be called when order is confirmed)
     * @param voucherId The voucher ID
     * @param userId The user ID
     * @param orderId The order ID
     * @param discountAmount The actual discount amount applied
     * @return The redemption record
     */
    @Transactional
    public VoucherRedemptions redeemVoucher(Long voucherId, Long userId, Long orderId, BigDecimal discountAmount) {
        // Update voucher usage count
        Vouchers voucher = vouchersRepository.findById(voucherId)
            .orElseThrow(() -> new IllegalArgumentException("Voucher không tồn tại"));

        int currentCount = voucher.getUsedCount() != null ? voucher.getUsedCount() : 0;
        voucher.setUsedCount(currentCount + 1);

        // Auto-expire if max uses reached
        if (voucher.getMaxUses() != null && voucher.getUsedCount() >= voucher.getMaxUses()) {
            voucher.setStatus("expired");
        }

        vouchersRepository.save(voucher);

        // Create redemption record
        VoucherRedemptions redemption = new VoucherRedemptions();
        redemption.setVoucherId(voucherId);
        redemption.setUserId(userId);
        redemption.setOrderId(orderId);
        redemption.setDiscountAmount(discountAmount);

        return redemptionsRepository.save(redemption);
    }

    /**
     * Get all active vouchers for a product
     */
    public List<Vouchers> getActiveVouchersForProduct(Long productId) {
        List<Vouchers> allVouchers = vouchersRepository.findAll();
        LocalDateTime now = LocalDateTime.now();

        return allVouchers.stream()
            .filter(v -> v.getProductId().equals(productId))
            .filter(v -> "active".equalsIgnoreCase(v.getStatus()))
            .filter(v -> v.getStartAt() == null || !now.isBefore(v.getStartAt()))
            .filter(v -> v.getEndAt() == null || !now.isAfter(v.getEndAt()))
            .filter(v -> v.getMaxUses() == null ||
                        (v.getUsedCount() != null ? v.getUsedCount() : 0) < v.getMaxUses())
            .toList();
    }

    /**
     * Get voucher usage statistics
     */
    public VoucherStatistics getVoucherStatistics(Long voucherId) {
        Vouchers voucher = vouchersRepository.findById(voucherId)
            .orElseThrow(() -> new IllegalArgumentException("Voucher không tồn tại"));

        List<VoucherRedemptions> redemptions = redemptionsRepository.findByVoucherId(voucherId);

        BigDecimal totalDiscountGiven = redemptions.stream()
            .map(VoucherRedemptions::getDiscountAmount)
            .filter(amount -> amount != null)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        long uniqueUsers = redemptions.stream()
            .map(VoucherRedemptions::getUserId)
            .filter(userId -> userId != null)
            .distinct()
            .count();

        return new VoucherStatistics(
            voucher.getUsedCount() != null ? voucher.getUsedCount() : 0,
            uniqueUsers,
            totalDiscountGiven,
            getRemainingUses(voucher)
        );
    }

    /**
     * Get remaining uses for a voucher
     */
    private Integer getRemainingUses(Vouchers voucher) {
        if (voucher.getMaxUses() == null) {
            return null; // Unlimited
        }
        int used = voucher.getUsedCount() != null ? voucher.getUsedCount() : 0;
        return Math.max(0, voucher.getMaxUses() - used);
    }

    /**
     * Auto-expire vouchers that have passed their end date
     */
    @Transactional
    public int autoExpireVouchers() {
        LocalDateTime now = LocalDateTime.now();
        List<Vouchers> activeVouchers = vouchersRepository.findAll().stream() //Lấy toàn bộ voucher trong database. //.stream() → Duyệt qua từng voucher dưới dạng luồng.
            .filter(v -> "active".equalsIgnoreCase(v.getStatus())) //Giữ lại chỉ những voucher đang ở trạng thái “active”
            .filter(v -> v.getEndAt() != null && now.isAfter(v.getEndAt())) //Giữ lại chỉ những voucher mà đã qua thời gian kết thúc
            .toList();//stream về lại dạng List.

        for (Vouchers voucher : activeVouchers) {
            voucher.setStatus("expired"); //Cập nhật trạng thái từ "active" → "expired".
            vouchersRepository.save(voucher);
        }

        return activeVouchers.size();//Cho biết có bao nhiêu voucher đã được cập nhật trong lần chạy nà
    }

    /**
     * Check if a voucher code already exists for a seller and product
     */
    public boolean isVoucherCodeExists(Long sellerId, Long productId, String code) {
        return vouchersRepository.existsBySellerIdAndProductIdAndCodeIgnoreCase(sellerId, productId, code);
    }

    /**
     * Create a new voucher with validation
     */
    @Transactional
    public Vouchers createVoucher(Vouchers voucher) {
        // Validate discount type
        if (voucher.getDiscountType() == null ||
            (!voucher.getDiscountType().equalsIgnoreCase("PERCENT") &&
             !voucher.getDiscountType().equalsIgnoreCase("AMOUNT"))) {
            throw new IllegalArgumentException("Loại giảm giá phải là PERCENT hoặc AMOUNT");
        }

        // Validate discount value
        if (voucher.getDiscountValue() == null || voucher.getDiscountValue().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Giá trị giảm giá phải lớn hơn 0");
        }

        if ("PERCENT".equalsIgnoreCase(voucher.getDiscountType()) &&
            voucher.getDiscountValue().compareTo(BigDecimal.valueOf(99)) > 0) {
            throw new IllegalArgumentException("Phần trăm giảm giá không được vượt quá 99%");
        }

        // Validate dates
        if (voucher.getStartAt() != null && voucher.getEndAt() != null &&
            voucher.getEndAt().isBefore(voucher.getStartAt())) {
            throw new IllegalArgumentException("Ngày kết thúc phải sau ngày bắt đầu");
        }

        // Check duplicate code
        if (isVoucherCodeExists(voucher.getSellerId(), voucher.getProductId(), voucher.getCode())) {
            throw new IllegalArgumentException("Mã voucher đã tồn tại");
        }

        return vouchersRepository.save(voucher);
    }

    /**
     * Update an existing voucher
     */
    @Transactional
    public Vouchers updateVoucher(Long voucherId, Vouchers updatedVoucher) {
        Vouchers existing = vouchersRepository.findById(voucherId)
            .orElseThrow(() -> new IllegalArgumentException("Voucher không tồn tại"));

        // Check if changing code would create duplicate (exclude current voucher)
        if (!existing.getCode().equalsIgnoreCase(updatedVoucher.getCode())) {
            // Only check if code is actually changing
            List<Vouchers> existingWithCode = vouchersRepository
                .findBySellerIdAndProductIdAndCodeIgnoreCase(
                    existing.getSellerId(),
                    existing.getProductId(),
                    updatedVoucher.getCode()
                );

            // Check if any voucher with this code exists (excluding current one)
            boolean duplicateExists = existingWithCode.stream()
                .anyMatch(v -> !v.getVoucherId().equals(voucherId));

            if (duplicateExists) {
                throw new IllegalArgumentException("Mã voucher đã tồn tại");
            }
        }

        // Update fields
        existing.setCode(updatedVoucher.getCode());
        existing.setDiscountType(updatedVoucher.getDiscountType());
        existing.setDiscountValue(updatedVoucher.getDiscountValue());
        existing.setMinOrder(updatedVoucher.getMinOrder());
        existing.setStartAt(updatedVoucher.getStartAt());
        existing.setEndAt(updatedVoucher.getEndAt());
        existing.setMaxUses(updatedVoucher.getMaxUses());
        existing.setMaxUsesPerUser(updatedVoucher.getMaxUsesPerUser());
        existing.setStatus(updatedVoucher.getStatus());

        return vouchersRepository.save(existing);
    }

    /**
     * Pause a voucher (set status to inactive)
     */
    @Transactional
    public Vouchers pauseVoucher(Long voucherId) {
        Vouchers voucher = vouchersRepository.findById(voucherId)
            .orElseThrow(() -> new IllegalArgumentException("Voucher không tồn tại"));

        if ("expired".equalsIgnoreCase(voucher.getStatus())) {
            throw new IllegalArgumentException("Không thể tạm dừng voucher đã hết hạn");
        }

        voucher.setStatus("inactive");
        return vouchersRepository.save(voucher);
    }

    /**
     * Resume a voucher (set status to active)
     */
    @Transactional
    public Vouchers resumeVoucher(Long voucherId) {
        Vouchers voucher = vouchersRepository.findById(voucherId)
            .orElseThrow(() -> new IllegalArgumentException("Voucher không tồn tại"));

        if ("expired".equalsIgnoreCase(voucher.getStatus())) {
            throw new IllegalArgumentException("Không thể kích hoạt lại voucher đã hết hạn");
        }

        // Check if voucher is already expired based on endDate
        if (voucher.getEndAt() != null && LocalDateTime.now().isAfter(voucher.getEndAt())) {
            voucher.setStatus("expired");
            vouchersRepository.save(voucher);
            throw new IllegalArgumentException("Voucher đã hết hạn, không thể kích hoạt lại");
        }

        voucher.setStatus("active");
        return vouchersRepository.save(voucher);
    }

    /**
     * Voucher statistics class
     */
    public static class VoucherStatistics {
        private final int totalUses;
        private final long uniqueUsers;
        private final BigDecimal totalDiscountGiven;
        private final Integer remainingUses;

        public VoucherStatistics(int totalUses, long uniqueUsers, BigDecimal totalDiscountGiven, Integer remainingUses) {
            this.totalUses = totalUses;
            this.uniqueUsers = uniqueUsers;
            this.totalDiscountGiven = totalDiscountGiven;
            this.remainingUses = remainingUses;
        }

        public int getTotalUses() { return totalUses; }
        public long getUniqueUsers() { return uniqueUsers; }
        public BigDecimal getTotalDiscountGiven() { return totalDiscountGiven; }
        public Integer getRemainingUses() { return remainingUses; }
    }

    /**
     * Format currency for display
     */
    private String formatCurrency(BigDecimal amount) {
        return String.format("%,.0f VNĐ", amount);
    }
}

