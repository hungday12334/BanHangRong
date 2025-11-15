package banhangrong.su25.Repository;

import banhangrong.su25.Entity.Vouchers;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface VouchersRepository extends JpaRepository<Vouchers, Long> {

    // Find vouchers by code (case-insensitive)
    List<Vouchers> findByCodeIgnoreCaseOrderByUpdatedAtDesc(String code);

    // Find vouchers by seller and product
    List<Vouchers> findBySellerIdAndProductIdOrderByCreatedAtDesc(Long sellerId, Long productId);
    List<Vouchers> findBySellerIdAndProductIdOrderByUpdatedAtDesc(Long sellerId, Long productId);

    // Find vouchers by seller and product with code search
    List<Vouchers> findBySellerIdAndProductIdAndCodeContainingIgnoreCaseOrderByCreatedAtDesc(Long sellerId, Long productId, String code);
    List<Vouchers> findBySellerIdAndProductIdAndCodeContainingIgnoreCaseOrderByUpdatedAtDesc(Long sellerId, Long productId, String code);

    // Find vouchers by seller only
    List<Vouchers> findBySellerIdOrderByCreatedAtDesc(Long sellerId);
    List<Vouchers> findBySellerIdOrderByUpdatedAtDesc(Long sellerId);

    // Check if voucher code exists for a seller and product
    boolean existsBySellerIdAndProductIdAndCodeIgnoreCase(Long sellerId, Long productId, String code);

    // Find by seller, product, and code
    List<Vouchers> findBySellerIdAndProductIdAndCodeIgnoreCase(Long sellerId, Long productId, String code);

    // Find active vouchers
    List<Vouchers> findByStatusIgnoreCase(String status);
    List<Vouchers> findByProductIdAndStatusIgnoreCase(Long productId, String status);

    // Find vouchers by date range
    @Query("SELECT v FROM Vouchers v WHERE v.endAt IS NOT NULL AND v.endAt < :now AND v.status = 'active'")
    List<Vouchers> findExpiredActiveVouchers(@Param("now") LocalDateTime now);

    // Find vouchers by seller and status
    List<Vouchers> findBySellerIdAndStatusIgnoreCaseOrderByUpdatedAtDesc(Long sellerId, String status);
    List<Vouchers> findBySellerIdAndStatusIgnoreCaseOrderByCreatedAtDesc(Long sellerId, String status);

    // Count vouchers by seller
    long countBySellerId(Long sellerId);
    long countBySellerIdAndStatusIgnoreCase(Long sellerId, String status);

    // Check if voucher code exists for a seller (regardless of product, status, or expiration)
    boolean existsBySellerIdAndCodeIgnoreCase(Long sellerId, String code);

    // Find vouchers by seller and code (regardless of product, status, or expiration)
    List<Vouchers> findBySellerIdAndCodeIgnoreCase(Long sellerId, String code);
}

