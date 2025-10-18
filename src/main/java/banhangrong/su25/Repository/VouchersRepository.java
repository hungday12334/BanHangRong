package banhangrong.su25.Repository;

import banhangrong.su25.Entity.Vouchers;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VouchersRepository extends JpaRepository<Vouchers, Long> {

    /**
     * Finds a page of vouchers for a specific seller and product.
     */
    Page<Vouchers> findBySellerIdAndProductId(Long sellerId, Long productId, Pageable pageable);

    /**
     * Finds a page of vouchers for a specific seller, product, and status.
     */
    Page<Vouchers> findBySellerIdAndProductIdAndStatus(Long sellerId, Long productId, String status, Pageable pageable);

    /**
     * Checks if a voucher with the given code already exists for a specific seller and product.
     */
    boolean existsBySellerIdAndProductIdAndCodeIgnoreCase(Long sellerId, Long productId, String code);
}
