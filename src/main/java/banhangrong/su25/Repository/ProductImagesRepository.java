package banhangrong.su25.Repository;

import banhangrong.su25.Entity.ProductImages;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface ProductImagesRepository extends JpaRepository<ProductImages, Long> {
    List<ProductImages> findTop1ByProductIdAndIsPrimaryTrueOrderByImageIdAsc(Long productId);
    List<ProductImages> findTop1ByProductIdOrderByImageIdAsc(Long productId);
    List<ProductImages> findByProductId(Long productId);

    @Modifying
    @Transactional
    @Query("DELETE FROM ProductImages pi WHERE pi.productId = :productId")
    int deleteByProductId(@Param("productId") Long productId);
}


