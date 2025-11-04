package banhangrong.su25.Repository;

import banhangrong.su25.Entity.CategoryLicenses;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface CategoryLicensesRepository extends JpaRepository<CategoryLicenses, Long> {

    // Find all licenses assigned to a category
    List<CategoryLicenses> findByCategoryIdAndSellerId(Long categoryId, Long sellerId);

    // Find all categories that have a specific license
    List<CategoryLicenses> findByShopLicenseIdAndSellerId(Long shopLicenseId, Long sellerId);

    // Check if a license is already assigned to a category
    boolean existsByCategoryIdAndShopLicenseId(Long categoryId, Long shopLicenseId);

    // Delete a specific assignment
    @Transactional
    @Modifying
    @Query("DELETE FROM CategoryLicenses cl WHERE cl.categoryId = :categoryId AND cl.shopLicenseId = :shopLicenseId AND cl.sellerId = :sellerId")
    void deleteByCategoryIdAndShopLicenseIdAndSellerId(@Param("categoryId") Long categoryId,
                                                         @Param("shopLicenseId") Long shopLicenseId,
                                                         @Param("sellerId") Long sellerId);

    // Delete all assignments for a category
    @Transactional
    @Modifying
    void deleteByCategoryIdAndSellerId(Long categoryId, Long sellerId);

    // Count licenses assigned to a category
    long countByCategoryIdAndSellerId(Long categoryId, Long sellerId);
}

