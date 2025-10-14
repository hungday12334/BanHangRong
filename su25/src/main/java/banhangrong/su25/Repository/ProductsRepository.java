package banhangrong.su25.Repository;

import banhangrong.su25.Entity.Products;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface ProductsRepository extends JpaRepository<Products, Long> {

    // === CÁC HÀM HIỆN CÓ ===
    List<Products> findBySellerId(Long sellerId);

    List<Products> findTop10BySellerIdAndIsActiveTrueAndQuantityLessThanEqualOrderByQuantityAsc(Long sellerId, Integer threshold);

    long countBySellerIdAndIsActiveTrue(Long sellerId);

    @Query(value = "SELECT COALESCE(SUM(oi.price_at_time * oi.quantity),0)\n" +
            "FROM order_items oi JOIN products p ON p.product_id = oi.product_id\n" +
            "WHERE p.seller_id = :sellerId", nativeQuery = true)
    BigDecimal totalRevenueBySeller(@Param("sellerId") Long sellerId);

    @Query(value = "SELECT COALESCE(SUM(oi.quantity),0)\n" +
            "FROM order_items oi JOIN products p ON p.product_id = oi.product_id\n" +
            "WHERE p.seller_id = :sellerId", nativeQuery = true)
    Long totalUnitsSoldBySeller(@Param("sellerId") Long sellerId);

    @Query(value = "SELECT CAST(oi.created_at AS DATE) as d, COALESCE(SUM(oi.price_at_time * oi.quantity),0) as revenue\n" +
            "FROM order_items oi JOIN products p ON p.product_id = oi.product_id\n" +
            "WHERE p.seller_id = :sellerId AND oi.created_at >= :fromDate\n" +
            "GROUP BY CAST(oi.created_at AS DATE)\n" +
            "ORDER BY CAST(oi.created_at AS DATE)", nativeQuery = true)
    List<Object[]> dailyRevenueFrom(@Param("sellerId") Long sellerId, @Param("fromDate") LocalDateTime fromDate);

    @Query(value = "SELECT COUNT(DISTINCT oi.order_id)\n" +
            "FROM order_items oi JOIN products p ON p.product_id = oi.product_id\n" +
            "WHERE p.seller_id = :sellerId", nativeQuery = true)
    Long totalOrdersBySeller(@Param("sellerId") Long sellerId);

    @Query(value = "SELECT COALESCE(AVG(pr.rating),0)\n" +
            "FROM product_reviews pr JOIN products p ON p.product_id = pr.product_id\n" +
            "WHERE p.seller_id = :sellerId", nativeQuery = true)
    BigDecimal averageRatingBySeller(@Param("sellerId") Long sellerId);

    @Query(value = "SELECT p.product_id, p.name, COALESCE(SUM(oi.quantity),0) AS units,\n" +
            "       COALESCE(SUM(oi.quantity * oi.price_at_time),0) AS revenue, COALESCE(AVG(pr.rating),0) AS rating\n" +
            "FROM products p\n" +
            "LEFT JOIN order_items oi ON oi.product_id = p.product_id\n" +
            "LEFT JOIN product_reviews pr ON pr.product_id = p.product_id\n" +
            "WHERE p.seller_id = :sellerId\n" +
            "GROUP BY p.product_id, p.name\n" +
            "ORDER BY revenue DESC\n" +
            "LIMIT 5", nativeQuery = true)
    List<Object[]> topProducts(@Param("sellerId") Long sellerId);

    @Query(value = "SELECT o.order_id, o.created_at, COALESCE(SUM(oi.quantity * oi.price_at_time),0) AS amount,\n" +
            "       COALESCE(SUM(oi.quantity),0) AS items\n" +
            "FROM orders o\n" +
            "JOIN order_items oi ON oi.order_id = o.order_id\n" +
            "JOIN products p ON p.product_id = oi.product_id\n" +
            "WHERE p.seller_id = :sellerId\n" +
            "GROUP BY o.order_id, o.created_at\n" +
            "ORDER BY o.created_at DESC\n" +
            "LIMIT 8", nativeQuery = true)
    List<Object[]> recentOrders(@Param("sellerId") Long sellerId);

    @Query(value = "SELECT COALESCE(SUM(oi.price_at_time * oi.quantity),0)\n" +
            "FROM order_items oi JOIN products p ON p.product_id = oi.product_id\n" +
            "WHERE p.seller_id = :sellerId AND CAST(oi.created_at AS DATE) = CURRENT_DATE", nativeQuery = true)
    BigDecimal todayRevenue(@Param("sellerId") Long sellerId);

    @Query(value = "SELECT COALESCE(SUM(oi.price_at_time * oi.quantity),0)\n" +
            "FROM order_items oi JOIN products p ON p.product_id = oi.product_id\n" +
            "WHERE p.seller_id = :sellerId AND YEAR(oi.created_at) = YEAR(CURRENT_DATE) AND MONTH(oi.created_at) = MONTH(CURRENT_DATE)", nativeQuery = true)
    BigDecimal thisMonthRevenue(@Param("sellerId") Long sellerId);

    // === CÁC HÀM MỚI CHO SHOP DESIGN ===

    // 1. Lấy sản phẩm bán chạy nhất với limit
    @Query("SELECT p FROM Products p WHERE p.sellerId = :sellerId AND p.isActive = true ORDER BY p.totalSales DESC")
    List<Products> findTopBestSellersBySellerId(@Param("sellerId") Long sellerId, Pageable pageable);

    // 2. Lấy sản phẩm đánh giá cao nhất với limit
    @Query("SELECT p FROM Products p WHERE p.sellerId = :sellerId AND p.isActive = true ORDER BY p.averageRating DESC")
    List<Products> findTopRatedBySellerId(@Param("sellerId") Long sellerId, Pageable pageable);

    // 3. Lấy sản phẩm mới nhất với limit
    @Query("SELECT p FROM Products p WHERE p.sellerId = :sellerId AND p.isActive = true ORDER BY p.createdAt DESC")
    List<Products> findNewArrivalsBySellerId(@Param("sellerId") Long sellerId, Pageable pageable);

    // 4. Native query cho filter tùy chỉnh
    @Query(value = """
        SELECT p.* FROM products p 
        LEFT JOIN categories_products cp ON p.product_id = cp.product_id 
        WHERE p.seller_id = :sellerId 
        AND p.is_active = true 
        AND (:categoryId IS NULL OR cp.category_id = :categoryId)
        AND (:minPrice IS NULL OR p.price >= :minPrice)
        AND (:maxPrice IS NULL OR p.price <= :maxPrice)
        ORDER BY p.created_at DESC 
        LIMIT :limit
        """, nativeQuery = true)
    List<Products> findWithCustomFilters(
            @Param("sellerId") Long sellerId,
            @Param("categoryId") Long categoryId,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            @Param("limit") int limit);

    // 5. Lấy sản phẩm theo ID list (cho featured products)
    @Query("SELECT p FROM Products p WHERE p.productId IN :productIds AND p.sellerId = :sellerId AND p.isActive = true")
    List<Products> findByProductIdInAndSellerId(@Param("productIds") List<Long> productIds, @Param("sellerId") Long sellerId);
}