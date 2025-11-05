package banhangrong.su25.Repository;

import banhangrong.su25.Entity.VoucherRedemptions;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface VoucherRedemptionsRepository extends JpaRepository<VoucherRedemptions, Long> {
    // Find redemptions by voucher
    List<VoucherRedemptions> findByVoucherId(Long voucherId);
    List<VoucherRedemptions> findByVoucherIdOrderByCreatedAtDesc(Long voucherId);

    // Find redemptions by user
    List<VoucherRedemptions> findByUserId(Long userId);
    List<VoucherRedemptions> findByUserIdOrderByCreatedAtDesc(Long userId);

    // Find redemption by order
    Optional<VoucherRedemptions> findByOrderId(Long orderId);

    // Count queries
    long countByVoucherId(Long voucherId);
    long countByVoucherIdAndUserId(Long voucherId, Long userId);
    long countByUserId(Long userId);

    // Check if order already has redemption
    boolean existsByOrderId(Long orderId);

    // Find redemptions in date range
    List<VoucherRedemptions> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    // Statistics query - total discount given by voucher
    @Query("SELECT SUM(r.discountAmount) FROM VoucherRedemptions r WHERE r.voucherId = :voucherId")
    java.math.BigDecimal sumDiscountAmountByVoucherId(@Param("voucherId") Long voucherId);

    // Statistics query - unique users count
    @Query("SELECT COUNT(DISTINCT r.userId) FROM VoucherRedemptions r WHERE r.voucherId = :voucherId")
    long countDistinctUsersByVoucherId(@Param("voucherId") Long voucherId);
}
