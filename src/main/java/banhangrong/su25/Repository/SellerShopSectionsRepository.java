package banhangrong.su25.Repository;

import banhangrong.su25.Entity.SellerShopSections;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SellerShopSectionsRepository extends JpaRepository<SellerShopSections, Long> {

    List<SellerShopSections> findBySellerIdAndIsActiveTrueOrderBySortOrderAsc(Long sellerId);

    @Query("SELECT s FROM SellerShopSections s WHERE s.sellerId = :sellerId ORDER BY s.sortOrder ASC")
    List<SellerShopSections> findAllBySellerId(@Param("sellerId") Long sellerId);

    void deleteBySellerId(Long sellerId);
}