package banhangrong.su25.Repository;

import banhangrong.su25.Entity.ProductReviews;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ProductReviewsRepository extends JpaRepository<ProductReviews, Long> {
    List<ProductReviews> findByProductIdOrderByCreatedAtDesc(Long productId);
    List<ProductReviews> findByUserIdOrderByCreatedAtDesc(Long userId);
    Page<ProductReviews> findByUserId(Long userId, Pageable pageable);
    List<ProductReviews> findByOrderItemId(Long orderItemId);
    Optional<ProductReviews> findByOrderItemIdAndUserId(Long orderItemId, Long userId);
    boolean existsByOrderItemId(Long orderItemId);
}


