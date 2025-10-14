package banhangrong.su25.Repository;

import banhangrong.su25.Entity.ProductReviews;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductReviewsRepository extends JpaRepository<ProductReviews, Long> {

    // Tìm tất cả review cho sản phẩm của seller (JOIN với products)
    @Query("SELECT pr FROM ProductReviews pr JOIN Products p ON pr.productId = p.productId WHERE p.sellerId = :sellerId ORDER BY pr.createdAt DESC")
    List<ProductReviews> findBySellerId(@Param("sellerId") Long sellerId);

    // Tìm review chưa được phản hồi
    @Query("SELECT pr FROM ProductReviews pr JOIN Products p ON pr.productId = p.productId WHERE p.sellerId = :sellerId AND pr.sellerResponse IS NULL ORDER BY pr.createdAt DESC")
    List<ProductReviews> findUnansweredReviews(@Param("sellerId") Long sellerId);

    // Đếm số review chưa phản hồi
    @Query("SELECT COUNT(pr) FROM ProductReviews pr JOIN Products p ON pr.productId = p.productId WHERE p.sellerId = :sellerId AND pr.sellerResponse IS NULL")
    Long countUnansweredReviews(@Param("sellerId") Long sellerId);

    // Tìm review theo productId
    List<ProductReviews> findByProductId(Long productId);

    // Tìm review theo userId
    List<ProductReviews> findByUserId(Long userId);
}