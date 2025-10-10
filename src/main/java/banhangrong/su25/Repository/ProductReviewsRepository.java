package banhangrong.su25.Repository;

import banhangrong.su25.Entity.ProductReviews;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ProductReviewsRepository extends JpaRepository<ProductReviews, Long> {
    List<ProductReviews> findByProductIdOrderByCreatedAtDesc(Long productId);
}


