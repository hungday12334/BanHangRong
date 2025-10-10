package banhangrong.su25.Repository;

import banhangrong.su25.Entity.ProductImages;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductImagesRepository extends JpaRepository<ProductImages, Long> {
    List<ProductImages> findTop1ByProductIdAndIsPrimaryTrueOrderByImageIdAsc(Long productId);
    List<ProductImages> findTop1ByProductIdOrderByImageIdAsc(Long productId);
}


