package banhangrong.su25.Controller;

import banhangrong.su25.Entity.VoucherRedemptions;
import banhangrong.su25.Entity.Vouchers;
import banhangrong.su25.Repository.VoucherRedemptionsRepository;
import banhangrong.su25.Repository.VouchersRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/api/seller/{sellerId}/products/{productId}/vouchers")
public class VouchersApiController {

    private final VouchersRepository vouchersRepository;
    private final VoucherRedemptionsRepository redemptionsRepository;

    public VouchersApiController(VouchersRepository vouchersRepository, VoucherRedemptionsRepository redemptionsRepository) {
        this.vouchersRepository = vouchersRepository;
        this.redemptionsRepository = redemptionsRepository;
    }

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
        return d;
    }

    @GetMapping
    public List<VoucherDto> list(@PathVariable Long sellerId, @PathVariable Long productId,
                                 @RequestParam(name = "q", required = false) String q) {
        List<Vouchers> base;
        if (q != null && !q.isBlank()) {
            base = vouchersRepository.findBySellerIdAndProductIdAndCodeContainingIgnoreCaseOrderByUpdatedAtDesc(sellerId, productId, q.trim());
        } else {
            base = vouchersRepository.findBySellerIdAndProductIdOrderByUpdatedAtDesc(sellerId, productId);
        }
        // Deduplicate by voucher code (case-insensitive). Keep the latest (list already ordered by updatedAt desc).
        java.util.LinkedHashMap<String, Vouchers> byCode = new java.util.LinkedHashMap<>();
        for (Vouchers v : base) {
            String key = v.getCode() == null ? "" : v.getCode().trim().toUpperCase();
            // only keep first occurrence (newest) per code
            byCode.putIfAbsent(key, v);
        }
        return byCode.values().stream().map(VouchersApiController::toDto).toList();
    }

    @PostMapping
    public ResponseEntity<?> create(@PathVariable Long sellerId, @PathVariable Long productId, @RequestBody VoucherDto body) {
        if (body.code == null || body.code.isBlank()) return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("code is required");
        if (vouchersRepository.existsBySellerIdAndProductIdAndCodeIgnoreCase(sellerId, productId, body.code)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("duplicate code");
        }
        Vouchers v = new Vouchers();
        v.setSellerId(sellerId);
        v.setProductId(productId);
        v.setCode(body.code.trim());
        v.setDiscountType(Objects.toString(body.discountType, "PERCENT").toUpperCase());
        v.setDiscountValue(body.discountValue != null ? body.discountValue : BigDecimal.ZERO);
        v.setMinOrder(body.minOrder);
        v.setStartAt(body.startAt);
        v.setEndAt(body.endAt);
        v.setMaxUses(body.maxUses);
        v.setMaxUsesPerUser(body.maxUsesPerUser);
        v.setStatus(Objects.toString(body.status, "active"));
        Vouchers saved = vouchersRepository.save(v);
        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(saved));
    }

    @PutMapping("/{voucherId}")
    public ResponseEntity<?> update(@PathVariable Long sellerId, @PathVariable Long productId, @PathVariable Long voucherId, @RequestBody VoucherDto body) {
        return vouchersRepository.findById(voucherId).map(v -> {
            if (!Objects.equals(v.getSellerId(), sellerId) || !Objects.equals(v.getProductId(), productId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Forbidden");
            }
            if (body.code != null && !body.code.isBlank() && !body.code.equalsIgnoreCase(v.getCode())) {
                if (vouchersRepository.existsBySellerIdAndProductIdAndCodeIgnoreCase(sellerId, productId, body.code)) {
                    return ResponseEntity.status(HttpStatus.CONFLICT).body("duplicate code");
                }
                v.setCode(body.code.trim());
            }
            if (body.discountType != null) v.setDiscountType(body.discountType.toUpperCase());
            if (body.discountValue != null) v.setDiscountValue(body.discountValue);
            v.setMinOrder(body.minOrder);
            v.setStartAt(body.startAt);
            v.setEndAt(body.endAt);
            v.setMaxUses(body.maxUses);
            v.setMaxUsesPerUser(body.maxUsesPerUser);
            if (body.status != null) v.setStatus(body.status);
            Vouchers saved = vouchersRepository.save(v);
            return ResponseEntity.ok(toDto(saved));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{voucherId}")
    public ResponseEntity<?> delete(@PathVariable Long sellerId, @PathVariable Long productId, @PathVariable Long voucherId) {
        return vouchersRepository.findById(voucherId).map(v -> {
            if (!Objects.equals(v.getSellerId(), sellerId) || !Objects.equals(v.getProductId(), productId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Forbidden");
            }
            vouchersRepository.deleteById(voucherId);
            return ResponseEntity.noContent().build();
        }).orElse(ResponseEntity.notFound().build());
    }

    public static class RedemptionDto {
        public Long redeemId;
        public Long orderId;
        public Long userId;
        public BigDecimal discountAmount;
        public LocalDateTime createdAt;
    }

    private static RedemptionDto toDto(VoucherRedemptions r) {
        RedemptionDto d = new RedemptionDto();
        d.redeemId = r.getRedeemId();
        d.orderId = r.getOrderId();
        d.userId = r.getUserId();
        d.discountAmount = r.getDiscountAmount();
        d.createdAt = r.getCreatedAt();
        return d;
    }

    @GetMapping("/{voucherId}/usage")
    public ResponseEntity<?> usage(@PathVariable Long sellerId, @PathVariable Long productId, @PathVariable Long voucherId) {
        return vouchersRepository.findById(voucherId).map(v -> {
            if (!Objects.equals(v.getSellerId(), sellerId) || !Objects.equals(v.getProductId(), productId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Forbidden");
            }
            List<RedemptionDto> list = redemptionsRepository.findByVoucherId(voucherId).stream().map(VouchersApiController::toDto).toList();
            return ResponseEntity.ok(list);
        }).orElse(ResponseEntity.notFound().build());
    }
}