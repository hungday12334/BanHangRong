package banhangrong.su25.Repository;

import banhangrong.su25.Entity.Vouchers;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VouchersRepository extends JpaRepository<Vouchers, Long> {
    List<Vouchers> findBySellerIdAndProductId(Long sellerId, Long productId);
    List<Vouchers> findBySellerIdAndProductIdOrderByUpdatedAtDesc(Long sellerId, Long productId);
    List<Vouchers> findBySellerIdAndProductIdAndCodeContainingIgnoreCaseOrderByUpdatedAtDesc(Long sellerId, Long productId, String code);
    boolean existsBySellerIdAndProductIdAndCodeIgnoreCase(Long sellerId, Long productId, String code);
}