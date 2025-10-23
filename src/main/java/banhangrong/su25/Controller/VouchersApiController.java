package banhangrong.su25.Controller;

import banhangrong.su25.Entity.VoucherRedemptions;
import banhangrong.su25.Entity.Vouchers;
import banhangrong.su25.Repository.VoucherRedemptionsRepository;
import banhangrong.su25.Repository.VouchersRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * Manages all API endpoints for seller vouchers.
 * This is the single source of truth for voucher API logic.
 */
@RestController
@RequestMapping("/api/seller/vouchers")
public class VouchersApiController {

    private final VouchersRepository vouchersRepository;
    private final VoucherRedemptionsRepository redemptionsRepository;

    public VouchersApiController(VouchersRepository vouchersRepository, VoucherRedemptionsRepository redemptionsRepository) {
        this.vouchersRepository = vouchersRepository;
        this.redemptionsRepository = redemptionsRepository;
    }

    private Long getCurrentSellerId(Authentication authentication) {
        // This is a placeholder. You must implement your own logic to get the seller ID.
        return 1L;
    }

    // DTO for Vouchers
    public static class VoucherDto { 
        private Long voucherId, productId;
        private String code, discountType, status;
        private BigDecimal discountValue, minOrder;
        private LocalDateTime startAt, endAt;
        private Integer maxUses, maxUsesPerUser, usedCount;

        // Getters & Setters
        public Long getVoucherId() { return voucherId; }
        public void setVoucherId(Long id) { this.voucherId = id; }
        public Long getProductId() { return productId; }
        public void setProductId(Long id) { this.productId = id; }
        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        public String getDiscountType() { return discountType; }
        public void setDiscountType(String type) { this.discountType = type; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public BigDecimal getDiscountValue() { return discountValue; }
        public void setDiscountValue(BigDecimal value) { this.discountValue = value; }
        public BigDecimal getMinOrder() { return minOrder; }
        public void setMinOrder(BigDecimal value) { this.minOrder = value; }
        public LocalDateTime getStartAt() { return startAt; }
        public void setStartAt(LocalDateTime at) { this.startAt = at; }
        public LocalDateTime getEndAt() { return endAt; }
        public void setEndAt(LocalDateTime at) { this.endAt = at; }
        public Integer getMaxUses() { return maxUses; }
        public void setMaxUses(Integer n) { this.maxUses = n; }
        public Integer getMaxUsesPerUser() { return maxUsesPerUser; }
        public void setMaxUsesPerUser(Integer n) { this.maxUsesPerUser = n; }
        public Integer getUsedCount() { return usedCount; }
        public void setUsedCount(Integer n) { this.usedCount = n; }
     }

    private VoucherDto toDto(Vouchers v) {
        VoucherDto dto = new VoucherDto();
        dto.setVoucherId(v.getVoucherId());
        dto.setProductId(v.getProductId());
        dto.setCode(v.getCode());
        dto.setDiscountType(v.getDiscountType());
        dto.setDiscountValue(v.getDiscountValue());
        dto.setMinOrder(v.getMinOrder());
        dto.setStartAt(v.getStartAt());
        dto.setEndAt(v.getEndAt());
        dto.setMaxUses(v.getMaxUses());
        dto.setMaxUsesPerUser(v.getMaxUsesPerUser());
        dto.setUsedCount(v.getUsedCount());
        dto.setStatus(v.getStatus());
        return dto;
    }

    @GetMapping
    public ResponseEntity<Page<VoucherDto>> getVouchers(
            @RequestParam Long productId,
            @RequestParam(required = false) String status,
            Pageable pageable,
            Authentication authentication) {
        Long sellerId = getCurrentSellerId(authentication);
        Page<Vouchers> vouchersPage = (status != null && !status.isBlank())
                ? vouchersRepository.findBySellerIdAndProductIdAndStatus(sellerId, productId, status, pageable)
                : vouchersRepository.findBySellerIdAndProductId(sellerId, productId, pageable);
        return ResponseEntity.ok(vouchersPage.map(this::toDto));
    }

    @GetMapping("/{voucherId}")
    public ResponseEntity<VoucherDto> getVoucherById(@PathVariable Long voucherId, Authentication authentication) {
        Long sellerId = getCurrentSellerId(authentication);
        return vouchersRepository.findById(voucherId)
                .filter(v -> v.getSellerId().equals(sellerId))
                .map(v -> ResponseEntity.ok(toDto(v)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> saveVoucher(@RequestBody VoucherDto dto, Authentication authentication) {
        if (dto.getVoucherId() == null) {
            return createVoucher(dto, authentication);
        } else {
            return updateVoucher(dto, authentication);
        }
    }

    private ResponseEntity<?> createVoucher(VoucherDto dto, Authentication authentication) {
        Long sellerId = getCurrentSellerId(authentication);

        if (dto.getProductId() == null) return ResponseEntity.badRequest().body("Product ID is required.");
        if (dto.getCode() == null || dto.getCode().isBlank()) return ResponseEntity.badRequest().body("Voucher code is required.");

        if (vouchersRepository.existsBySellerIdAndProductIdAndCodeIgnoreCase(sellerId, dto.getProductId(), dto.getCode())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Voucher code already exists.");
        }

        Vouchers newVoucher = new Vouchers();
        newVoucher.setSellerId(sellerId);
        newVoucher.setProductId(dto.getProductId());
        newVoucher.setCode(dto.getCode());
        newVoucher.setDiscountType(Objects.toString(dto.getDiscountType(), "PERCENT"));
        newVoucher.setDiscountValue(Objects.requireNonNullElse(dto.getDiscountValue(), BigDecimal.ZERO));
        newVoucher.setStatus(Objects.toString(dto.getStatus(), "active"));
        newVoucher.setMinOrder(dto.getMinOrder());
        newVoucher.setStartAt(dto.getStartAt());
        newVoucher.setEndAt(dto.getEndAt());
        newVoucher.setMaxUses(dto.getMaxUses());
        newVoucher.setMaxUsesPerUser(dto.getMaxUsesPerUser());
        
        Vouchers savedVoucher = vouchersRepository.save(newVoucher);
        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(savedVoucher));
    }

    private ResponseEntity<?> updateVoucher(VoucherDto dto, Authentication authentication) {
        Long sellerId = getCurrentSellerId(authentication);

        Vouchers existingVoucher = vouchersRepository.findById(dto.getVoucherId()).orElse(null);
        if (existingVoucher == null) return ResponseEntity.notFound().build();
        if (!existingVoucher.getSellerId().equals(sellerId)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();

        // Safely update code: only if it's provided and different
        if (dto.getCode() != null && !dto.getCode().isBlank() && !dto.getCode().equalsIgnoreCase(existingVoucher.getCode())) {
            if (vouchersRepository.existsBySellerIdAndProductIdAndCodeIgnoreCase(sellerId, existingVoucher.getProductId(), dto.getCode())) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body("Voucher code already exists.");
            }
            existingVoucher.setCode(dto.getCode());
        }

        // Update other fields only if they are provided in the DTO
        if (dto.getDiscountType() != null) existingVoucher.setDiscountType(dto.getDiscountType());
        if (dto.getDiscountValue() != null) existingVoucher.setDiscountValue(dto.getDiscountValue());
        if (dto.getStatus() != null) existingVoucher.setStatus(dto.getStatus());
        
        // These fields can be intentionally set to null
        existingVoucher.setMinOrder(dto.getMinOrder());
        existingVoucher.setStartAt(dto.getStartAt());
        existingVoucher.setEndAt(dto.getEndAt());
        existingVoucher.setMaxUses(dto.getMaxUses());
        existingVoucher.setMaxUsesPerUser(dto.getMaxUsesPerUser());

        Vouchers updatedVoucher = vouchersRepository.save(existingVoucher);
        return ResponseEntity.ok(toDto(updatedVoucher));
    }

    @DeleteMapping("/{voucherId}")
    public ResponseEntity<Void> deleteVoucher(@PathVariable Long voucherId, Authentication authentication) {
        Long sellerId = getCurrentSellerId(authentication);
        return vouchersRepository.findById(voucherId)
                .filter(v -> v.getSellerId().equals(sellerId))
                .map(v -> {
                    vouchersRepository.delete(v);
                    return ResponseEntity.noContent().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{voucherId}/redemptions")
    public ResponseEntity<List<VoucherRedemptions>> getVoucherRedemptions(@PathVariable Long voucherId, Authentication authentication) {
        Long sellerId = getCurrentSellerId(authentication);
        return vouchersRepository.findById(voucherId)
                .filter(v -> v.getSellerId().equals(sellerId))
                .map(v -> ResponseEntity.ok(redemptionsRepository.findByVoucherId(v.getVoucherId())))
                .orElse(ResponseEntity.notFound().build());
    }
}
