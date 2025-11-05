package banhangrong.su25.Controller;

import banhangrong.su25.Entity.VoucherRedemptions;
import banhangrong.su25.Entity.Vouchers;
import banhangrong.su25.Repository.VoucherRedemptionsRepository;
import banhangrong.su25.Repository.VouchersRepository;
import banhangrong.su25.service.VoucherService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * Professional Voucher Management API for Sellers
 * Manages CRUD operations for vouchers and provides usage statistics
 */
@RestController
@RequestMapping("/api/seller/{sellerId}/products/{productId}/vouchers")
public class VouchersApiController {

    private final VouchersRepository vouchersRepository;
    private final VoucherRedemptionsRepository redemptionsRepository;
    private final VoucherService voucherService;

    public VouchersApiController(VouchersRepository vouchersRepository,
                                VoucherRedemptionsRepository redemptionsRepository,
                                VoucherService voucherService) {
        this.vouchersRepository = vouchersRepository;
        this.redemptionsRepository = redemptionsRepository;
        this.voucherService = voucherService;
    }

    /**
     * DTO for voucher responses
     */
    public static class VoucherDto {
        public Long voucherId;
        public String code;
        public String discountType; // PERCENT | AMOUNT
        public BigDecimal discountValue;
        public BigDecimal minOrder;
        public LocalDateTime startAt;
        public LocalDateTime endAt;
        public Integer maxUses;
        public Integer maxUsesPerUser;
        public Integer usedCount;
        public Integer remainingUses;
        public String status;
        public LocalDateTime createdAt;
        public LocalDateTime updatedAt;
    }

    /**
     * DTO for voucher statistics
     */
    public static class VoucherStatsDto {
        public int totalUses;
        public long uniqueUsers;
        public BigDecimal totalDiscountGiven;
        public Integer remainingUses;
        public String status;
    }

    private static VoucherDto toDto(Vouchers v) {
        VoucherDto d = new VoucherDto();
        d.voucherId = v.getVoucherId();
        d.code = v.getCode();
        d.discountType = v.getDiscountType();
        d.discountValue = v.getDiscountValue();
        d.minOrder = v.getMinOrder();
        d.startAt = v.getStartAt();
        d.endAt = v.getEndAt();
        d.maxUses = v.getMaxUses();
        d.maxUsesPerUser = v.getMaxUsesPerUser();
        d.usedCount = v.getUsedCount();
        d.status = v.getStatus();
        d.createdAt = v.getCreatedAt();
        d.updatedAt = v.getUpdatedAt();

        // Calculate remaining uses
        if (v.getMaxUses() != null) {
            int used = v.getUsedCount() != null ? v.getUsedCount() : 0;
            d.remainingUses = Math.max(0, v.getMaxUses() - used);
        }

        return d;
    }

    /**
     * List all vouchers for a product
     * @param sellerId Seller ID
     * @param productId Product ID
     * @param q Optional search query for voucher code
     * @param status Optional filter by status (active, inactive, expired)
     * @return List of vouchers
     */
    @GetMapping
    public ResponseEntity<?> list(@PathVariable Long sellerId,
                                  @PathVariable Long productId,
                                  @RequestParam(name = "q", required = false) String q,
                                  @RequestParam(name = "status", required = false) String status) {
        try {
            List<Vouchers> base;

            if (q != null && !q.isBlank()) {
                base = vouchersRepository.findBySellerIdAndProductIdAndCodeContainingIgnoreCaseOrderByUpdatedAtDesc(
                    sellerId, productId, q.trim());
            } else {
                base = vouchersRepository.findBySellerIdAndProductIdOrderByUpdatedAtDesc(sellerId, productId);
            }

            // Filter by status if provided
            if (status != null && !status.isBlank()) {
                base = base.stream()
                    .filter(v -> status.equalsIgnoreCase(v.getStatus()))
                    .toList();
            }

            // Deduplicate by voucher code (case-insensitive). Keep the latest.
            java.util.LinkedHashMap<String, Vouchers> byCode = new java.util.LinkedHashMap<>();
            for (Vouchers v : base) {
                String key = v.getCode() == null ? "" : v.getCode().trim().toUpperCase();
                byCode.putIfAbsent(key, v);
            }

            List<VoucherDto> result = byCode.values().stream().map(VouchersApiController::toDto).toList();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error: " + e.getMessage());
        }
    }

    /**
     * Create a new voucher
     */
    @PostMapping
    public ResponseEntity<?> create(@PathVariable Long sellerId,
                                   @PathVariable Long productId,
                                   @RequestBody VoucherDto body) {
        try {
            // Validation
            if (body.code == null || body.code.isBlank()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Mã voucher không được để trống");
            }

            if (body.discountType == null ||
                (!body.discountType.equalsIgnoreCase("PERCENT") && !body.discountType.equalsIgnoreCase("AMOUNT"))) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Loại giảm giá phải là PERCENT hoặc AMOUNT");
            }

            if (body.discountValue == null || body.discountValue.compareTo(BigDecimal.ZERO) <= 0) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Giá trị giảm giá phải lớn hơn 0");
            }

            if ("PERCENT".equalsIgnoreCase(body.discountType) &&
                body.discountValue.compareTo(BigDecimal.valueOf(100)) > 0) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Phần trăm giảm giá không được vượt quá 100%");
            }

            // Check duplicate
            if (vouchersRepository.existsBySellerIdAndProductIdAndCodeIgnoreCase(sellerId, productId, body.code)) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body("Mã voucher đã tồn tại");
            }

            // Create voucher
            Vouchers v = new Vouchers();
            v.setSellerId(sellerId);
            v.setProductId(productId);
            v.setCode(body.code.trim().toUpperCase());
            v.setDiscountType(body.discountType.toUpperCase());
            v.setDiscountValue(body.discountValue);
            v.setMinOrder(body.minOrder);
            v.setStartAt(body.startAt);
            v.setEndAt(body.endAt);
            v.setMaxUses(body.maxUses);
            v.setMaxUsesPerUser(body.maxUsesPerUser);
            v.setStatus(Objects.toString(body.status, "active"));

            Vouchers saved = voucherService.createVoucher(v);
            return ResponseEntity.status(HttpStatus.CREATED).body(toDto(saved));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Lỗi khi tạo voucher: " + e.getMessage());
        }
    }

    /**
     * Update an existing voucher
     */
    @PutMapping("/{voucherId}")
    public ResponseEntity<?> update(@PathVariable Long sellerId,
                                   @PathVariable Long productId,
                                   @PathVariable Long voucherId,
                                   @RequestBody VoucherDto body) {
        try {
            return vouchersRepository.findById(voucherId).map(v -> {
                // Verify ownership
                if (!Objects.equals(v.getSellerId(), sellerId) || !Objects.equals(v.getProductId(), productId)) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Bạn không có quyền chỉnh sửa voucher này");
                }

                // Update code if changed
                if (body.code != null && !body.code.isBlank() && !body.code.equalsIgnoreCase(v.getCode())) {
                    if (vouchersRepository.existsBySellerIdAndProductIdAndCodeIgnoreCase(sellerId, productId, body.code)) {
                        return ResponseEntity.status(HttpStatus.CONFLICT).body("Mã voucher đã tồn tại");
                    }
                    v.setCode(body.code.trim().toUpperCase());
                }

                // Update other fields
                if (body.discountType != null) v.setDiscountType(body.discountType.toUpperCase());
                if (body.discountValue != null) {
                    if (body.discountValue.compareTo(BigDecimal.ZERO) <= 0) {
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body("Giá trị giảm giá phải lớn hơn 0");
                    }
                    v.setDiscountValue(body.discountValue);
                }

                v.setMinOrder(body.minOrder);
                v.setStartAt(body.startAt);
                v.setEndAt(body.endAt);
                v.setMaxUses(body.maxUses);
                v.setMaxUsesPerUser(body.maxUsesPerUser);
                if (body.status != null) v.setStatus(body.status);

                Vouchers saved = vouchersRepository.save(v);
                return ResponseEntity.ok(toDto(saved));
            }).orElse(ResponseEntity.notFound().build());

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Lỗi khi cập nhật voucher: " + e.getMessage());
        }
    }

    /**
     * Delete a voucher
     */
    @DeleteMapping("/{voucherId}")
    public ResponseEntity<?> delete(@PathVariable Long sellerId,
                                   @PathVariable Long productId,
                                   @PathVariable Long voucherId) {
        try {
            return vouchersRepository.findById(voucherId).map(v -> {
                if (!Objects.equals(v.getSellerId(), sellerId) || !Objects.equals(v.getProductId(), productId)) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Bạn không có quyền xóa voucher này");
                }

                // Check if voucher has been used
                long usageCount = redemptionsRepository.countByVoucherId(voucherId);
                if (usageCount > 0) {
                    // Don't delete, just deactivate
                    v.setStatus("inactive");
                    vouchersRepository.save(v);
                    return ResponseEntity.ok("Voucher đã được vô hiệu hóa vì đã có người sử dụng");
                }

                vouchersRepository.deleteById(voucherId);
                return ResponseEntity.noContent().build();
            }).orElse(ResponseEntity.notFound().build());

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Lỗi khi xóa voucher: " + e.getMessage());
        }
    }

    /**
     * DTO for redemption records
     */
    public static class RedemptionDto {
        public Long redeemId;
        public Long orderId;
        public Long userId;
        public BigDecimal discountAmount;
        public LocalDateTime createdAt;
    }

    private static RedemptionDto toRedemptionDto(VoucherRedemptions r) {
        RedemptionDto d = new RedemptionDto();
        d.redeemId = r.getRedeemId();
        d.orderId = r.getOrderId();
        d.userId = r.getUserId();
        d.discountAmount = r.getDiscountAmount();
        d.createdAt = r.getCreatedAt();
        return d;
    }

    /**
     * Get voucher usage history
     */
    @GetMapping("/{voucherId}/usage")
    public ResponseEntity<?> usage(@PathVariable Long sellerId,
                                  @PathVariable Long productId,
                                  @PathVariable Long voucherId) {
        try {
            return vouchersRepository.findById(voucherId).map(v -> {
                if (!Objects.equals(v.getSellerId(), sellerId) || !Objects.equals(v.getProductId(), productId)) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Bạn không có quyền xem thông tin này");
                }

                List<RedemptionDto> list = redemptionsRepository
                    .findByVoucherIdOrderByCreatedAtDesc(voucherId)
                    .stream()
                    .map(VouchersApiController::toRedemptionDto)
                    .toList();

                return ResponseEntity.ok(list);
            }).orElse(ResponseEntity.notFound().build());

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Lỗi khi lấy lịch sử sử dụng: " + e.getMessage());
        }
    }

    /**
     * Get voucher statistics
     */
    @GetMapping("/{voucherId}/stats")
    public ResponseEntity<?> stats(@PathVariable Long sellerId,
                                  @PathVariable Long productId,
                                  @PathVariable Long voucherId) {
        try {
            return vouchersRepository.findById(voucherId).map(v -> {
                if (!Objects.equals(v.getSellerId(), sellerId) || !Objects.equals(v.getProductId(), productId)) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Bạn không có quyền xem thông tin này");
                }

                VoucherService.VoucherStatistics stats = voucherService.getVoucherStatistics(voucherId);

                VoucherStatsDto dto = new VoucherStatsDto();
                dto.totalUses = stats.getTotalUses();
                dto.uniqueUsers = stats.getUniqueUsers();
                dto.totalDiscountGiven = stats.getTotalDiscountGiven();
                dto.remainingUses = stats.getRemainingUses();
                dto.status = v.getStatus();

                return ResponseEntity.ok(dto);
            }).orElse(ResponseEntity.notFound().build());

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Lỗi khi lấy thống kê: " + e.getMessage());
        }
    }

    /**
     * Bulk activate/deactivate vouchers
     */
    @PostMapping("/bulk-status")
    public ResponseEntity<?> bulkUpdateStatus(@PathVariable Long sellerId,
                                             @PathVariable Long productId,
                                             @RequestBody BulkStatusRequest request) {
        try {
            if (request.voucherIds == null || request.voucherIds.isEmpty()) {
                return ResponseEntity.badRequest().body("Vui lòng chọn ít nhất một voucher");
            }

            if (request.status == null || request.status.isBlank()) {
                return ResponseEntity.badRequest().body("Trạng thái không hợp lệ");
            }

            int updated = 0;
            for (Long voucherId : request.voucherIds) {
                vouchersRepository.findById(voucherId).ifPresent(v -> {
                    if (Objects.equals(v.getSellerId(), sellerId) && Objects.equals(v.getProductId(), productId)) {
                        v.setStatus(request.status);
                        vouchersRepository.save(v);
                    }
                });
                updated++;
            }

            return ResponseEntity.ok("Đã cập nhật " + updated + " voucher");

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Lỗi khi cập nhật voucher: " + e.getMessage());
        }
    }

    public static class BulkStatusRequest {
        public List<Long> voucherIds;
        public String status;
    }
}