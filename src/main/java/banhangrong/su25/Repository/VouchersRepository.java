package banhangrong.su25.Repository;

import banhangrong.su25.Entity.Vouchers;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface VouchersRepository extends JpaRepository<Vouchers, Long> {
    // Seller queries
    List<Vouchers> findBySellerIdAndProductId(Long sellerId, Long productId);
    List<Vouchers> findBySellerIdAndProductIdOrderByUpdatedAtDesc(Long sellerId, Long productId);
    List<Vouchers> findBySellerIdAndProductIdAndCodeContainingIgnoreCaseOrderByUpdatedAtDesc(Long sellerId, Long productId, String code);
    List<Vouchers> findBySellerIdAndProductIdAndCodeIgnoreCase(Long sellerId, Long productId, String code);
    List<Vouchers> findBySellerIdOrderByUpdatedAtDesc(Long sellerId);
    boolean existsBySellerIdAndProductIdAndCodeIgnoreCase(Long sellerId, Long productId, String code);

    // Customer-facing: look up voucher by code
    List<Vouchers> findByCodeIgnoreCaseOrderByUpdatedAtDesc(String code);
    Optional<Vouchers> findTopByCodeIgnoreCaseOrderByUpdatedAtDesc(String code);

    // Find active vouchers
    List<Vouchers> findByStatusIgnoreCase(String status);
    List<Vouchers> findByProductIdAndStatusIgnoreCase(Long productId, String status);

    // Find vouchers by date range
    @Query("SELECT v FROM Vouchers v WHERE v.endAt IS NOT NULL AND v.endAt < :now AND v.status = 'active'")
    List<Vouchers> findExpiredActiveVouchers(@Param("now") LocalDateTime now);

    // Find vouchers by seller and status
    List<Vouchers> findBySellerIdAndStatusIgnoreCaseOrderByUpdatedAtDesc(Long sellerId, String status);

    // Count vouchers by seller
    long countBySellerId(Long sellerId);
    long countBySellerIdAndStatusIgnoreCase(Long sellerId, String status);
}