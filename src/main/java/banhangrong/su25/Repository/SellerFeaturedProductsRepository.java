// SellerFeaturedProductsRepository.java - CẦN SỬA
package banhangrong.su25.Repository;

import banhangrong.su25.Entity.SellerFeaturedProducts;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface SellerFeaturedProductsRepository extends JpaRepository<SellerFeaturedProducts, Long> {

    List<SellerFeaturedProducts> findBySectionSectionIdOrderBySortOrderAsc(Long sectionId);

    @Transactional
    @Modifying
    @Query("DELETE FROM SellerFeaturedProducts s WHERE s.section.sectionId = :sectionId")
    void deleteBySectionSectionId(@Param("sectionId") Long sectionId);

    boolean existsBySectionSectionIdAndProductProductId(Long sectionId, Long productId);
}
