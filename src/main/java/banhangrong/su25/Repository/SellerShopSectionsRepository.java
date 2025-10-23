// SellerShopSectionsRepository.java - CẦN SỬA
package banhangrong.su25.Repository;

import banhangrong.su25.Entity.SectionType;
import banhangrong.su25.Entity.SellerShopSections;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface SellerShopSectionsRepository extends JpaRepository<SellerShopSections, Long> {

    List<SellerShopSections> findBySellerIdAndIsActiveTrueOrderBySortOrderAsc(Long sellerId);

    @Query("SELECT s FROM SellerShopSections s WHERE s.sellerId = :sellerId ORDER BY s.sortOrder ASC")
    List<SellerShopSections> findAllBySellerId(@Param("sellerId") Long sellerId);

    @Transactional
    @Modifying
    @Query("DELETE FROM SellerShopSections s WHERE s.sellerId = :sellerId")
    void deleteBySellerId(@Param("sellerId") Long sellerId);

    boolean existsBySellerIdAndSectionType(Long sellerId, SectionType sectionType);
}