package banhangrong.su25.Repository;

import banhangrong.su25.Entity.SellerFeaturedProducts;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SellerFeaturedProductsRepository extends JpaRepository<SellerFeaturedProducts, Long> {
    List<SellerFeaturedProducts> findBySectionSectionIdOrderBySortOrderAsc(Long sectionId);
    void deleteBySectionSectionId(Long sectionId);
}