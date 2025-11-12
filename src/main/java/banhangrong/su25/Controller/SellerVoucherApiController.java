package banhangrong.su25.Controller;

import banhangrong.su25.Entity.Users;
import banhangrong.su25.Entity.Vouchers;
import banhangrong.su25.Entity.VoucherRedemptions;
import banhangrong.su25.Entity.Products;
import banhangrong.su25.Repository.UsersRepository;
import banhangrong.su25.Repository.VouchersRepository;
import banhangrong.su25.Repository.VoucherRedemptionsRepository;
import banhangrong.su25.Repository.ProductsRepository;
import banhangrong.su25.service.VoucherService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Seller API Controller for Voucher Management
 * Handles all API endpoints for seller voucher operations
 */
@RestController
@RequestMapping("/api/seller/vouchers")
public class SellerVoucherApiController {

    private final VoucherService voucherService;
    private final VouchersRepository vouchersRepository;
    private final VoucherRedemptionsRepository redemptionsRepository;
    private final ProductsRepository productsRepository;
    private final UsersRepository usersRepository;

    public SellerVoucherApiController(VoucherService voucherService,
                                     VouchersRepository vouchersRepository,
                                     VoucherRedemptionsRepository redemptionsRepository,
                                     ProductsRepository productsRepository,
                                     UsersRepository usersRepository) {
        this.voucherService = voucherService;
        this.vouchersRepository = vouchersRepository;
        this.redemptionsRepository = redemptionsRepository;
        this.productsRepository = productsRepository;
        this.usersRepository = usersRepository;
    }

    /**
     * Get current authenticated seller
     */
    private Users getCurrentSeller() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            return usersRepository.findByUsername(auth.getName()).orElse(null);
        }
        return null;
    }

    /**
     * Voucher DTO with product information
     */
    public static class VoucherWithProductDto {
        public Long voucherId;
        public String code;
        public String discountType;
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
        public ProductInfo product;

        public static class ProductInfo {
            public Long productId;
            public String name;
            public String imageUrl;
        }
    }

    /**
     * Get all vouchers for current seller
     */
    @GetMapping
    public ResponseEntity<?> getSellerVouchers(
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            Users seller = getCurrentSeller();
            if (seller == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Vui lòng đăng nhập");
            }

            // Auto-expire vouchers first
            voucherService.autoExpireVouchers();

            List<Vouchers> vouchers;

            if (productId != null) {
                // Get vouchers for specific product - sorted by createdAt DESC (newest first)
                if (search != null && !search.trim().isEmpty()) {
                    vouchers = vouchersRepository.findBySellerIdAndProductIdAndCodeContainingIgnoreCaseOrderByCreatedAtDesc(
                        seller.getUserId(), productId, search.trim());
                } else {
                    // Get all vouchers for specific product
                    vouchers = vouchersRepository.findBySellerIdAndProductIdOrderByCreatedAtDesc(
                        seller.getUserId(), productId);
                }
            } else {
                // Get all vouchers for seller - sorted by createdAt DESC (newest first)
                vouchers = vouchersRepository.findBySellerIdOrderByCreatedAtDesc(seller.getUserId());

                if (search != null && !search.trim().isEmpty()) {
                    String searchLower = search.trim().toLowerCase();
                    vouchers = vouchers.stream() //.stream() biến vouchers thành luồng dữ liệu để xử lý.
                        .filter(v -> v.getCode().toLowerCase().contains(searchLower)) //lọc các voucher có mã chứa chuỗi tìm kiếm (không phân biệt chữ hoa/thường).
                        .collect(Collectors.toList()); //thu thập các voucher đã lọc thành một danh sách mới.
                }
            }

            // Filter by status
            if (status != null && !status.trim().isEmpty()) {
                vouchers = vouchers.stream()
                    .filter(v -> status.equalsIgnoreCase(v.getStatus()))
                    .collect(Collectors.toList());
            }

            // Map vouchers to DTOs with product info
            List<VoucherWithProductDto> result = vouchers.stream().map(v -> {
                VoucherWithProductDto dto = new VoucherWithProductDto();
                dto.voucherId = v.getVoucherId();
                dto.code = v.getCode();
                dto.discountType = v.getDiscountType();
                dto.discountValue = v.getDiscountValue();
                dto.minOrder = v.getMinOrder();
                dto.startAt = v.getStartAt();
                dto.endAt = v.getEndAt();
                dto.maxUses = v.getMaxUses();
                dto.maxUsesPerUser = v.getMaxUsesPerUser();
                dto.usedCount = v.getUsedCount();
                dto.status = v.getStatus();
                dto.createdAt = v.getCreatedAt();
                dto.updatedAt = v.getUpdatedAt();

                // Tính toán lượng sử dụng còn lại
                if (v.getMaxUses() != null) {
                    int used = v.getUsedCount() != null ? v.getUsedCount() : 0;
                    dto.remainingUses = Math.max(0, v.getMaxUses() - used);
                }

                // Get product info
                productsRepository.findById(v.getProductId()).ifPresent(p -> { // Tìm sản phẩm theo productId của voucher.
                    VoucherWithProductDto.ProductInfo pInfo = new VoucherWithProductDto.ProductInfo(); // Tạo một đối tượng ProductInfo mới để lưu trữ thông tin sản phẩm.
                    pInfo.productId = p.getProductId();
                    pInfo.name = p.getName();
                    // Get product image would go here
                    dto.product = pInfo;
                });

                return dto;
            }).collect(Collectors.toList());

            // Apply pagination
            int start = page * size;
            int end = Math.min(start + size, result.size());
            List<VoucherWithProductDto> paginatedResult = start < result.size()
                ? result.subList(start, end)
                : List.of();

            Map<String, Object> response = new HashMap<>();
            response.put("content", paginatedResult);
            response.put("totalElements", result.size());
            response.put("totalPages", (int) Math.ceil((double) result.size() / size));
            response.put("currentPage", page);
            response.put("size", size);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Lỗi khi lấy danh sách voucher: " + e.getMessage());
        }
    }

    /**
     * Get voucher by ID
     */
    @GetMapping("/{voucherId}")
    public ResponseEntity<?> getVoucher(@PathVariable Long voucherId) {
        try {
            Users seller = getCurrentSeller();
            if (seller == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Vui lòng đăng nhập");
            }

            return vouchersRepository.findById(voucherId)
                .map(v -> {
                    if (!v.getSellerId().equals(seller.getUserId())) {
                        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body("Bạn không có quyền xem voucher này");
                    }

                    VoucherWithProductDto dto = new VoucherWithProductDto();
                    dto.voucherId = v.getVoucherId();
                    dto.code = v.getCode();
                    dto.discountType = v.getDiscountType();
                    dto.discountValue = v.getDiscountValue();
                    dto.minOrder = v.getMinOrder();
                    dto.startAt = v.getStartAt();
                    dto.endAt = v.getEndAt();
                    dto.maxUses = v.getMaxUses();
                    dto.maxUsesPerUser = v.getMaxUsesPerUser();
                    dto.usedCount = v.getUsedCount();
                    dto.status = v.getStatus();
                    dto.createdAt = v.getCreatedAt();
                    dto.updatedAt = v.getUpdatedAt();

                    if (v.getMaxUses() != null) {
                        int used = v.getUsedCount() != null ? v.getUsedCount() : 0;
                        dto.remainingUses = Math.max(0, v.getMaxUses() - used);
                    }

                    return ResponseEntity.ok(dto);
                })
                .orElse(ResponseEntity.notFound().build());

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Lỗi khi lấy thông tin voucher: " + e.getMessage());
        }
    }

    /**
     * Create voucher request
     */
    public static class CreateVoucherRequest {
        public Long productId;
        public String code;
        public String discountType;
        public BigDecimal discountValue;
        public BigDecimal minOrder;
        public LocalDateTime startAt;
        public LocalDateTime endAt;
        public Integer maxUses;
        public Integer maxUsesPerUser;
        public String status; // Added status field
    }

    /**
     * Create a new voucher
     */
    @PostMapping
    public ResponseEntity<?> createVoucher(@RequestBody CreateVoucherRequest request) {
        try {
            Users seller = getCurrentSeller();
            if (seller == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Vui lòng đăng nhập");
            }

            // Validate product ownership
            Products product = productsRepository.findById(request.productId)
                .orElseThrow(() -> new IllegalArgumentException("Sản phẩm không tồn tại")); // Tìm sản phẩm theo productId từ yêu cầu.

            if (!product.getSellerId().equals(seller.getUserId())) { // Kiểm tra xem người bán hiện tại có phải là chủ sở hữu sản phẩm không.
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Bạn không có quyền tạo voucher cho sản phẩm này");
            }

            // Validate maxUses: REQUIRED, must be integer from 1-1000
            if (request.maxUses == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Số lần sử dụng tối đa không được để trống");
            }
            if (request.maxUses < 1 || request.maxUses > 1000) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Số lần sử dụng tối đa phải từ 1 đến 1000");
            }

            // Validate maxUsesPerUser: REQUIRED, must be integer from 1-1000
            if (request.maxUsesPerUser == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Số lần sử dụng tối đa/người không được để trống");
            }
            if (request.maxUsesPerUser < 1 || request.maxUsesPerUser > 1000) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Số lần sử dụng tối đa/người phải từ 1 đến 1000");
            }

            // Validate relationship: maxUsesPerUser <= maxUses
            if (request.maxUsesPerUser > request.maxUses) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Số lần sử dụng/người không được vượt quá tổng số lần sử dụng");
            }

            // Validate minOrder: optional but if provided, must be >= 0 and <= 1,000,000
            if (request.minOrder != null) {
                if (request.minOrder.compareTo(BigDecimal.ZERO) < 0) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Giá trị đơn hàng tối thiểu phải >= 0");
                }
                if (request.minOrder.compareTo(new BigDecimal("1000000")) > 0) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Giá trị đơn hàng tối thiểu không được vượt quá 1,000,000 VNĐ");
                }
                // For AMOUNT type, minOrder must be >= discountValue
                if ("AMOUNT".equalsIgnoreCase(request.discountType) && request.discountValue != null) {
                    if (request.minOrder.compareTo(request.discountValue) < 0) {
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body("Giá trị đơn hàng tối thiểu phải >= giá trị giảm giá (với loại AMOUNT)");
                    }
                }
            }

            // Validate date range
            if (request.startAt != null && request.endAt != null &&
                request.endAt.isBefore(request.startAt)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Ngày kết thúc phải sau ngày bắt đầu");
            }

            // Check if already expired
            String initialStatus = request.status != null ? request.status : "inactive"; //request nó ở đâu?
            if (request.endAt != null && LocalDateTime.now().isAfter(request.endAt)) {
                initialStatus = "expired";
            }

            // Create voucher
            Vouchers voucher = new Vouchers();
            voucher.setSellerId(seller.getUserId());
            voucher.setProductId(request.productId);
            voucher.setCode(request.code.trim().toUpperCase());
            voucher.setDiscountType(request.discountType.toUpperCase());
            voucher.setDiscountValue(request.discountValue);
            voucher.setMinOrder(request.minOrder);
            voucher.setStartAt(request.startAt);
            voucher.setEndAt(request.endAt);
            voucher.setMaxUses(request.maxUses);
            voucher.setMaxUsesPerUser(request.maxUsesPerUser);
            voucher.setStatus(initialStatus); // Use status from request or default to inactive

            Vouchers saved = voucherService.createVoucher(voucher);

            return ResponseEntity.status(HttpStatus.CREATED).body(saved); // Trả về đối tượng voucher đã lưu với mã trạng thái 201 Created.

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Lỗi khi tạo voucher: " + e.getMessage());
        }
    }

    /**
     * Update a voucher
     */
    @PutMapping("/{voucherId}")
    public ResponseEntity<?> updateVoucher(@PathVariable Long voucherId,
                                          @RequestBody CreateVoucherRequest request) {
        try {
            Users seller = getCurrentSeller();
            if (seller == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Vui lòng đăng nhập");
            }

            Vouchers existing = vouchersRepository.findById(voucherId)
                .orElseThrow(() -> new IllegalArgumentException("Voucher không tồn tại"));

            if (!existing.getSellerId().equals(seller.getUserId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Bạn không có quyền chỉnh sửa voucher này");
            }

            // ✅ BACKEND VALIDATION - BẮT BUỘC (Security layer)
            // Validate maxUses: REQUIRED, must be integer from 1-1000
            if (request.maxUses == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Số lần sử dụng tối đa không được để trống");
            }
            if (request.maxUses < 1 || request.maxUses > 1000) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Số lần sử dụng tối đa phải từ 1 đến 1000");
            }

            // Validate maxUsesPerUser: REQUIRED, must be integer from 1-1000
            if (request.maxUsesPerUser == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Số lần sử dụng tối đa/người không được để trống");
            }
            if (request.maxUsesPerUser < 1 || request.maxUsesPerUser > 1000) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Số lần sử dụng tối đa/người phải từ 1 đến 1000");
            }

            // Validate relationship: maxUsesPerUser <= maxUses
            if (request.maxUsesPerUser > request.maxUses) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Số lần sử dụng/người không được vượt quá tổng số lần sử dụng");
            }

            // Validate minOrder: optional but if provided, must be >= 0 and <= 1,000,000
            if (request.minOrder != null) {
                if (request.minOrder.compareTo(BigDecimal.ZERO) < 0) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Giá trị đơn hàng tối thiểu phải >= 0");
                }
                if (request.minOrder.compareTo(new BigDecimal("1000000")) > 0) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Giá trị đơn hàng tối thiểu không được vượt quá 1,000,000 VNĐ");
                }
                // For AMOUNT type, minOrder must be >= discountValue
                if ("AMOUNT".equalsIgnoreCase(request.discountType) && request.discountValue != null) {
                    if (request.minOrder.compareTo(request.discountValue) < 0) {
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body("Giá trị đơn hàng tối thiểu phải >= giá trị giảm giá (với loại AMOUNT)");
                    }
                }
            }

            // Validate date range
            if (request.startAt != null && request.endAt != null &&
                request.endAt.isBefore(request.startAt)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Ngày kết thúc phải sau ngày bắt đầu");
            }

            // Update voucher
            Vouchers updated = new Vouchers();
            updated.setCode(request.code.trim().toUpperCase());
            updated.setDiscountType(request.discountType.toUpperCase());
            updated.setDiscountValue(request.discountValue);
            updated.setMinOrder(request.minOrder);
            updated.setStartAt(request.startAt);
            updated.setEndAt(request.endAt);
            updated.setMaxUses(request.maxUses);
            updated.setMaxUsesPerUser(request.maxUsesPerUser);

            // Keep status from request if provided, otherwise keep existing status
            if (request.status != null && !request.status.trim().isEmpty()) {
                // Auto-expire if end date is past
                if (request.endAt != null && LocalDateTime.now().isAfter(request.endAt)) {
                    updated.setStatus("expired");
                } else {
                    updated.setStatus(request.status);
                }
            } else {
                updated.setStatus(existing.getStatus());
            }

            Vouchers saved = voucherService.updateVoucher(voucherId, updated);

            return ResponseEntity.ok(saved);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Lỗi khi cập nhật voucher: " + e.getMessage());
        }
    }

    /**
     * Delete/deactivate a voucher
     */
    @DeleteMapping("/{voucherId}")
    public ResponseEntity<?> deleteVoucher(@PathVariable Long voucherId) {
        try {
            Users seller = getCurrentSeller();
            if (seller == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Vui lòng đăng nhập");
            }

            Vouchers voucher = vouchersRepository.findById(voucherId)
                .orElseThrow(() -> new IllegalArgumentException("Voucher không tồn tại"));

            if (!voucher.getSellerId().equals(seller.getUserId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Bạn không có quyền xóa voucher này");
            }

            // Check if voucher has been used
            long usageCount = redemptionsRepository.countByVoucherId(voucherId);
            if (usageCount > 0) {
                // Don't delete, just deactivate
                voucher.setStatus("inactive");
                vouchersRepository.save(voucher);
                return ResponseEntity.ok("Voucher đã được vô hiệu hóa");
            }

            vouchersRepository.deleteById(voucherId);
            return ResponseEntity.ok("Voucher đã được xóa");

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Lỗi khi xóa voucher: " + e.getMessage());
        }
    }

    /**
     * Quick update voucher status (inline editing)
     */
    @PatchMapping("/{voucherId}/status")
    public ResponseEntity<?> updateVoucherStatus(@PathVariable Long voucherId,
                                                  @RequestBody Map<String, String> request) {
        try {
            Users seller = getCurrentSeller();
            if (seller == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Vui lòng đăng nhập");
            }

            String newStatus = request.get("status");
            if (newStatus == null || newStatus.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Status không được để trống");
            }

            newStatus = newStatus.trim().toLowerCase();
            if (!newStatus.equals("active") && !newStatus.equals("inactive") && !newStatus.equals("expired")) {
                return ResponseEntity.badRequest().body("Status phải là active, inactive, hoặc expired");
            }

            Vouchers voucher = vouchersRepository.findById(voucherId)
                .orElseThrow(() -> new IllegalArgumentException("Voucher không tồn tại"));

            if (!voucher.getSellerId().equals(seller.getUserId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Bạn không có quyền chỉnh sửa voucher này");
            }

            // Check if trying to activate expired voucher
            if (newStatus.equals("active") && voucher.getEndAt() != null
                && LocalDateTime.now().isAfter(voucher.getEndAt())) {
                return ResponseEntity.badRequest()
                    .body("Không thể kích hoạt voucher đã hết hạn");
            }

            voucher.setStatus(newStatus);
            Vouchers saved = vouchersRepository.save(voucher);

            return ResponseEntity.ok(saved);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Lỗi khi cập nhật status: " + e.getMessage());
        }
    }

    /**
     * Pause a voucher (set status to inactive)
     */
    @PostMapping("/{voucherId}/pause")
    public ResponseEntity<?> pauseVoucher(@PathVariable Long voucherId) {
        try {
            Users seller = getCurrentSeller();
            if (seller == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Vui lòng đăng nhập");
            }

            Vouchers voucher = vouchersRepository.findById(voucherId)
                .orElseThrow(() -> new IllegalArgumentException("Voucher không tồn tại"));

            if (!voucher.getSellerId().equals(seller.getUserId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Bạn không có quyền tạm dừng voucher này");
            }

            Vouchers paused = voucherService.pauseVoucher(voucherId);
            return ResponseEntity.ok(paused);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Lỗi khi tạm dừng voucher: " + e.getMessage());
        }
    }

    /**
     * Resume a voucher (set status to active)
     */
    @PostMapping("/{voucherId}/resume")
    public ResponseEntity<?> resumeVoucher(@PathVariable Long voucherId) {
        try {
            Users seller = getCurrentSeller();
            if (seller == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Vui lòng đăng nhập");
            }

            Vouchers voucher = vouchersRepository.findById(voucherId)
                .orElseThrow(() -> new IllegalArgumentException("Voucher không tồn tại"));

            if (!voucher.getSellerId().equals(seller.getUserId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Bạn không có quyền kích hoạt lại voucher này");
            }

            Vouchers resumed = voucherService.resumeVoucher(voucherId);
            return ResponseEntity.ok(resumed);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Lỗi khi kích hoạt lại voucher: " + e.getMessage());
        }
    }

    /**
     * Get voucher redemption history
     */
    @GetMapping("/{voucherId}/redemptions")
    public ResponseEntity<?> getRedemptions(@PathVariable Long voucherId) {
        try {
            Users seller = getCurrentSeller();
            if (seller == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Vui lòng đăng nhập");
            }

            Vouchers voucher = vouchersRepository.findById(voucherId)
                .orElseThrow(() -> new IllegalArgumentException("Voucher không tồn tại"));

            if (!voucher.getSellerId().equals(seller.getUserId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Bạn không có quyền xem thông tin này");
            }

            List<VoucherRedemptions> redemptions = redemptionsRepository
                .findByVoucherIdOrderByCreatedAtDesc(voucherId);

            return ResponseEntity.ok(redemptions);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Lỗi khi lấy lịch sử sử dụng: " + e.getMessage());
        }
    }

    /**
     * Get voucher statistics
     */
    @GetMapping("/{voucherId}/statistics")
    public ResponseEntity<?> getStatistics(@PathVariable Long voucherId) {
        try {
            Users seller = getCurrentSeller();
            if (seller == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Vui lòng đăng nhập");
            }

            Vouchers voucher = vouchersRepository.findById(voucherId)
                .orElseThrow(() -> new IllegalArgumentException("Voucher không tồn tại"));

            if (!voucher.getSellerId().equals(seller.getUserId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Bạn không có quyền xem thông tin này");
            }

            VoucherService.VoucherStatistics stats = voucherService.getVoucherStatistics(voucherId);

            return ResponseEntity.ok(stats);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Lỗi khi lấy thống kê: " + e.getMessage());
        }
    }

    /**
     * Get seller's products for voucher creation
     */
    @GetMapping("/products")
    public ResponseEntity<?> getSellerProducts() {
        try {
            Users seller = getCurrentSeller();
            if (seller == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Vui lòng đăng nhập");
            }

            List<Products> products = productsRepository.findBySellerId(seller.getUserId());

            System.out.println("Seller ID: " + seller.getUserId() + ", Found products: " + products.size());

            List<Map<String, Object>> result = products.stream()
                .filter(p -> {
                    // Include all products, not just Public status
                    boolean isValidStatus = p.getStatus() != null &&
                        (p.getStatus().equalsIgnoreCase("Public") ||
                         p.getStatus().equalsIgnoreCase("public"));
                    System.out.println("Product: " + p.getName() + ", Status: " + p.getStatus() + ", Valid: " + isValidStatus);
                    return isValidStatus;
                })
                .map(p -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("productId", p.getProductId());
                    map.put("name", p.getName());
                    map.put("price", p.getPrice());
                    map.put("salePrice", p.getSalePrice());
                    map.put("status", p.getStatus());
                    map.put("quantity", p.getQuantity());
                    return map;
                })
                .collect(Collectors.toList()); // Thu thập các bản đồ sản phẩm đã lọc thành một danh sách.

            System.out.println("Returning " + result.size() + " products to frontend");

            return ResponseEntity.ok(result); // Trả về danh sách sản phẩm đã lọc dưới dạng phản hồi HTTP.

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Lỗi khi lấy danh sách sản phẩm: " + e.getMessage());
        }
    }

    /**
     * Get dashboard statistics for seller
     */
    @GetMapping("/dashboard")
    public ResponseEntity<?> getDashboard() {
        try {
            Users seller = getCurrentSeller();
            if (seller == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Vui lòng đăng nhập");
            }

            List<Vouchers> allVouchers = vouchersRepository.findBySellerIdOrderByUpdatedAtDesc(seller.getUserId());

            long activeCount = allVouchers.stream().filter(v -> "active".equalsIgnoreCase(v.getStatus())).count();
            long inactiveCount = allVouchers.stream().filter(v -> "inactive".equalsIgnoreCase(v.getStatus())).count();
            long expiredCount = allVouchers.stream().filter(v -> "expired".equalsIgnoreCase(v.getStatus())).count();

            int totalRedemptions = allVouchers.stream()
                .mapToInt(v -> v.getUsedCount() != null ? v.getUsedCount() : 0)
                .sum();

            Map<String, Object> dashboard = new HashMap<>();
            dashboard.put("totalVouchers", allVouchers.size());
            dashboard.put("activeVouchers", activeCount);
            dashboard.put("inactiveVouchers", inactiveCount);
            dashboard.put("expiredVouchers", expiredCount);
            dashboard.put("totalRedemptions", totalRedemptions);

            return ResponseEntity.ok(dashboard);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Lỗi khi lấy thống kê: " + e.getMessage());
        }
    }
}

