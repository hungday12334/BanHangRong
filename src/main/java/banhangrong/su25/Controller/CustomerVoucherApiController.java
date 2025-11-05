package banhangrong.su25.Controller;

import banhangrong.su25.Entity.Users;
import banhangrong.su25.Entity.Vouchers;
import banhangrong.su25.Repository.UsersRepository;
import banhangrong.su25.Repository.VouchersRepository;
import banhangrong.su25.Repository.ProductsRepository;
import banhangrong.su25.service.VoucherService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Customer API for voucher validation and application
 * This controller handles customer-facing voucher operations
 */
@RestController
@RequestMapping("/api/customer/vouchers")
public class CustomerVoucherApiController {

    private final VoucherService voucherService;
    private final VouchersRepository vouchersRepository;
    private final ProductsRepository productsRepository;
    private final UsersRepository usersRepository;

    public CustomerVoucherApiController(VoucherService voucherService,
                                       VouchersRepository vouchersRepository,
                                       ProductsRepository productsRepository,
                                       UsersRepository usersRepository) {
        this.voucherService = voucherService;
        this.vouchersRepository = vouchersRepository;
        this.productsRepository = productsRepository;
        this.usersRepository = usersRepository;
    }

    /**
     * Get current authenticated user
     */
    private Users getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            return usersRepository.findByUsername(auth.getName()).orElse(null);
        }
        return null;
    }

    /**
     * DTO for available vouchers
     */
    public static class AvailableVoucherDto {
        public Long voucherId;
        public String code;
        public String discountType;
        public BigDecimal discountValue;
        public BigDecimal minOrder;
        public LocalDateTime endAt;
        public Integer remainingUses;
        public Long productId;
        public String productName;
        public String productImage;
        public boolean canUse;
        public String description;
    }

    /**
     * Get all available vouchers for a product
     */
    @GetMapping("/product/{productId}")
    public ResponseEntity<?> getAvailableVouchers(@PathVariable Long productId) {
        try {
            Users user = getCurrentUser();
            List<Vouchers> vouchers = voucherService.getActiveVouchersForProduct(productId);

            List<AvailableVoucherDto> result = vouchers.stream().map(v -> {
                AvailableVoucherDto dto = new AvailableVoucherDto();
                dto.voucherId = v.getVoucherId();
                dto.code = v.getCode();
                dto.discountType = v.getDiscountType();
                dto.discountValue = v.getDiscountValue();
                dto.minOrder = v.getMinOrder();
                dto.endAt = v.getEndAt();
                dto.productId = v.getProductId();

                // Calculate remaining uses
                if (v.getMaxUses() != null) {
                    int used = v.getUsedCount() != null ? v.getUsedCount() : 0;
                    dto.remainingUses = Math.max(0, v.getMaxUses() - used);
                }

                // Get product info
                productsRepository.findById(productId).ifPresent(p -> {
                    dto.productName = p.getName();
                });

                // Check if user can use this voucher
                dto.canUse = true;
                if (user != null && v.getMaxUsesPerUser() != null) {
                    // This would need to check user's usage history
                    // For now, we'll keep it simple
                }

                // Create description
                if ("PERCENT".equalsIgnoreCase(v.getDiscountType())) {
                    dto.description = String.format("Giảm %s%%", v.getDiscountValue());
                } else {
                    dto.description = String.format("Giảm %,.0f VNĐ", v.getDiscountValue());
                }

                if (v.getMinOrder() != null && v.getMinOrder().compareTo(BigDecimal.ZERO) > 0) {
                    dto.description += String.format(" cho đơn tối thiểu %,.0f VNĐ", v.getMinOrder());
                }

                return dto;
            }).collect(Collectors.toList());

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Lỗi khi lấy danh sách voucher: " + e.getMessage());
        }
    }

    /**
     * Request to validate a voucher
     */
    public static class ValidateVoucherRequest {
        public String voucherCode;
        public Long productId;
        public BigDecimal orderAmount;
    }

    /**
     * Response for voucher validation
     */
    public static class ValidateVoucherResponse {
        public boolean valid;
        public String errorCode;
        public String errorMessage;
        public BigDecimal discountAmount;
        public BigDecimal finalAmount;
        public VoucherDetails voucher;

        public static class VoucherDetails {
            public Long voucherId;
            public String code;
            public String discountType;
            public BigDecimal discountValue;
        }
    }

    /**
     * Validate a voucher code for a specific order
     * This endpoint allows customers to check if a voucher is valid before checkout
     */
    @PostMapping("/validate")
    public ResponseEntity<?> validateVoucher(@RequestBody ValidateVoucherRequest request) {
        try {
            Users user = getCurrentUser();
            Long userId = user != null ? user.getUserId() : null;

            // Validate input
            if (request.voucherCode == null || request.voucherCode.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Mã voucher không được để trống");
            }

            if (request.productId == null) {
                return ResponseEntity.badRequest().body("Thiếu thông tin sản phẩm");
            }

            if (request.orderAmount == null || request.orderAmount.compareTo(BigDecimal.ZERO) <= 0) {
                return ResponseEntity.badRequest().body("Số tiền đơn hàng không hợp lệ");
            }

            // Validate voucher
            VoucherService.VoucherValidationResult result = voucherService.validateAndCalculateDiscount(
                request.voucherCode,
                userId,
                request.productId,
                request.orderAmount
            );

            ValidateVoucherResponse response = new ValidateVoucherResponse();
            response.valid = result.isValid();
            response.errorCode = result.getErrorCode();
            response.errorMessage = result.getErrorMessage();

            if (result.isValid()) {
                response.discountAmount = result.getDiscountAmount();
                response.finalAmount = request.orderAmount.subtract(result.getDiscountAmount());

                // Add voucher details
                ValidateVoucherResponse.VoucherDetails details = new ValidateVoucherResponse.VoucherDetails();
                details.voucherId = result.getVoucher().getVoucherId();
                details.code = result.getVoucher().getCode();
                details.discountType = result.getVoucher().getDiscountType();
                details.discountValue = result.getVoucher().getDiscountValue();
                response.voucher = details;
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Lỗi khi kiểm tra voucher: " + e.getMessage());
        }
    }

    /**
     * Get all available vouchers (for browsing)
     */
    @GetMapping("/browse")
    public ResponseEntity<?> browseVouchers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Integer limit) {
        try {
            LocalDateTime now = LocalDateTime.now();
            List<Vouchers> allVouchers = vouchersRepository.findByStatusIgnoreCase("active");

            // Filter by search term
            if (search != null && !search.trim().isEmpty()) {
                String searchLower = search.trim().toLowerCase();
                allVouchers = allVouchers.stream()
                    .filter(v -> v.getCode().toLowerCase().contains(searchLower))
                    .collect(Collectors.toList());
            }

            // Filter by type
            if (type != null && !type.trim().isEmpty()) {
                allVouchers = allVouchers.stream()
                    .filter(v -> type.equalsIgnoreCase(v.getDiscountType()))
                    .collect(Collectors.toList());
            }

            // Filter by date range and availability
            allVouchers = allVouchers.stream()
                .filter(v -> v.getStartAt() == null || !now.isBefore(v.getStartAt()))
                .filter(v -> v.getEndAt() == null || !now.isAfter(v.getEndAt()))
                .filter(v -> {
                    if (v.getMaxUses() == null) return true;
                    int used = v.getUsedCount() != null ? v.getUsedCount() : 0;
                    return used < v.getMaxUses();
                })
                .collect(Collectors.toList());

            // Apply limit
            if (limit != null && limit > 0 && allVouchers.size() > limit) {
                allVouchers = allVouchers.subList(0, limit);
            }

            // Convert to DTO
            List<AvailableVoucherDto> result = allVouchers.stream().map(v -> {
                AvailableVoucherDto dto = new AvailableVoucherDto();
                dto.voucherId = v.getVoucherId();
                dto.code = v.getCode();
                dto.discountType = v.getDiscountType();
                dto.discountValue = v.getDiscountValue();
                dto.minOrder = v.getMinOrder();
                dto.endAt = v.getEndAt();
                dto.productId = v.getProductId();
                dto.canUse = true;

                if (v.getMaxUses() != null) {
                    int used = v.getUsedCount() != null ? v.getUsedCount() : 0;
                    dto.remainingUses = Math.max(0, v.getMaxUses() - used);
                }

                // Get product info
                productsRepository.findById(v.getProductId()).ifPresent(p -> {
                    dto.productName = p.getName();
                });

                // Create description
                if ("PERCENT".equalsIgnoreCase(v.getDiscountType())) {
                    dto.description = String.format("Giảm %s%%", v.getDiscountValue());
                } else {
                    dto.description = String.format("Giảm %,.0f VNĐ", v.getDiscountValue());
                }

                return dto;
            }).collect(Collectors.toList());

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Lỗi khi lấy danh sách voucher: " + e.getMessage());
        }
    }

    /**
     * Check voucher availability by code
     */
    @GetMapping("/check/{code}")
    public ResponseEntity<?> checkVoucher(@PathVariable String code) {
        try {
            List<Vouchers> vouchers = vouchersRepository.findByCodeIgnoreCaseOrderByUpdatedAtDesc(code);

            if (vouchers.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Không tìm thấy mã voucher này");
            }

            Vouchers voucher = vouchers.get(0);

            // Check basic availability
            if (!"active".equalsIgnoreCase(voucher.getStatus())) {
                return ResponseEntity.ok(new CheckVoucherResponse(false, "Mã voucher không còn hiệu lực"));
            }

            LocalDateTime now = LocalDateTime.now();
            if (voucher.getStartAt() != null && now.isBefore(voucher.getStartAt())) {
                return ResponseEntity.ok(new CheckVoucherResponse(false, "Mã voucher chưa có hiệu lực"));
            }

            if (voucher.getEndAt() != null && now.isAfter(voucher.getEndAt())) {
                return ResponseEntity.ok(new CheckVoucherResponse(false, "Mã voucher đã hết hạn"));
            }

            if (voucher.getMaxUses() != null) {
                int used = voucher.getUsedCount() != null ? voucher.getUsedCount() : 0;
                if (used >= voucher.getMaxUses()) {
                    return ResponseEntity.ok(new CheckVoucherResponse(false, "Voucher đã hết lượt sử dụng"));
                }
            }

            return ResponseEntity.ok(new CheckVoucherResponse(true, "Voucher hợp lệ", voucher));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Lỗi khi kiểm tra voucher: " + e.getMessage());
        }
    }

    public static class CheckVoucherResponse {
        public boolean available;
        public String message;
        public VoucherInfo voucher;

        public CheckVoucherResponse(boolean available, String message) {
            this.available = available;
            this.message = message;
        }

        public CheckVoucherResponse(boolean available, String message, Vouchers v) {
            this.available = available;
            this.message = message;
            if (v != null) {
                this.voucher = new VoucherInfo();
                this.voucher.code = v.getCode();
                this.voucher.discountType = v.getDiscountType();
                this.voucher.discountValue = v.getDiscountValue();
                this.voucher.minOrder = v.getMinOrder();
                this.voucher.endAt = v.getEndAt();
                this.voucher.productId = v.getProductId();
            }
        }

        public static class VoucherInfo {
            public String code;
            public String discountType;
            public BigDecimal discountValue;
            public BigDecimal minOrder;
            public LocalDateTime endAt;
            public Long productId;
        }
    }
}

