package banhangrong.su25.Repository;

import banhangrong.su25.Entity.ShopLicenses;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ShopLicensesRepository extends JpaRepository<ShopLicenses, Long> {
    List<ShopLicenses> findBySellerIdOrderByCreatedAtDesc(Long sellerId);

    List<ShopLicenses> findBySellerIdAndIsActive(Long sellerId, Boolean isActive);

    List<ShopLicenses> findBySellerIdAndStatus(Long sellerId, String status);

    long countBySellerIdAndIsActive(Long sellerId, Boolean isActive);
}

