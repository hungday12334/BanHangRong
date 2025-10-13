package banhangrong.su25.Repository;

import banhangrong.su25.Entity.Products;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProductsRepository extends JpaRepository<Products, Long> {
        // Tìm sản phẩm theo status (phân trang)
        Page<Products> findByStatus(String status, Pageable pageable);
    // Tìm sản phẩm theo tên hoặc mô tả (case-insensitive) và status (phân trang)
    @Query(value = "SELECT p FROM Products p WHERE (LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%')) OR LOWER(p.description) LIKE LOWER(CONCAT('%', :desc, '%'))) AND LOWER(p.status) = LOWER(:status)",
            countQuery = "SELECT COUNT(p) FROM Products p WHERE (LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%')) OR LOWER(p.description) LIKE LOWER(CONCAT('%', :desc, '%'))) AND LOWER(p.status) = LOWER(:status)")
    Page<Products> findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCaseAndStatus(@Param("name") String name, @Param("desc") String desc, @Param("status") String status, Pageable pageable);
    // Tìm sản phẩm theo categoryId và status (phân trang)
    @Query(value = "SELECT p FROM Products p WHERE LOWER(p.status) = LOWER(:status) AND EXISTS (SELECT 1 FROM CategoriesProducts cp WHERE cp.id.productId = p.productId AND cp.id.categoryId = :categoryId)",
            countQuery = "SELECT COUNT(p) FROM Products p WHERE LOWER(p.status) = LOWER(:status) AND EXISTS (SELECT 1 FROM CategoriesProducts cp WHERE cp.id.productId = p.productId AND cp.id.categoryId = :categoryId)")
    Page<Products> findByCategoryIdAndStatus(@Param("categoryId") Long categoryId, @Param("status") String status, Pageable pageable);
    // Tìm sản phẩm theo categoryId, status và từ khóa tìm kiếm (phân trang)
    @Query(value = "SELECT p FROM Products p WHERE LOWER(p.status) = LOWER(:status) AND (LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(p.description) LIKE LOWER(CONCAT('%', :search, '%'))) AND EXISTS (SELECT 1 FROM CategoriesProducts cp WHERE cp.id.productId = p.productId AND cp.id.categoryId = :categoryId)",
            countQuery = "SELECT COUNT(p) FROM Products p WHERE LOWER(p.status) = LOWER(:status) AND (LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(p.description) LIKE LOWER(CONCAT('%', :search, '%'))) AND EXISTS (SELECT 1 FROM CategoriesProducts cp WHERE cp.id.productId = p.productId AND cp.id.categoryId = :categoryId)")
    Page<Products> findByCategoryIdAndStatusAndSearch(@Param("categoryId") Long categoryId, @Param("status") String status, @Param("search") String search, org.springframework.data.domain.Pageable pageable);
    List<Products> findBySellerId(Long sellerId);

        @Query("SELECT p FROM Products p WHERE p.sellerId = :sellerId AND LOWER(p.status) = LOWER(:status) AND p.quantity <= :threshold ORDER BY p.quantity ASC")
        List<Products> findTop10BySellerIdAndStatusAndQuantityLessThanEqualOrderByQuantityAsc(@Param("sellerId") Long sellerId, @Param("status") String status, @Param("threshold") Integer threshold);

        @Query("SELECT COUNT(p) FROM Products p WHERE p.sellerId = :sellerId AND LOWER(p.status) = LOWER(:status)")
        long countBySellerIdAndStatus(@Param("sellerId") Long sellerId, @Param("status") String status);

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

    // Seller revenue ranking (total lifetime)
    @Query(value = "SELECT p.seller_id, u.username, COALESCE(SUM(oi.price_at_time * oi.quantity),0) AS revenue, COALESCE(SUM(oi.quantity),0) AS units\n" +
            "FROM products p\n" +
            "JOIN users u ON u.user_id = p.seller_id\n" +
            "LEFT JOIN order_items oi ON oi.product_id = p.product_id\n" +
            "GROUP BY p.seller_id, u.username\n" +
            "ORDER BY revenue DESC\n" +
            "LIMIT 10", nativeQuery = true)
    List<Object[]> topSellers();

    // Rank (1-based) of a seller by revenue across all sellers
    @Query(value = "SELECT r.rank FROM (\n" +
            "  SELECT p.seller_id, DENSE_RANK() OVER (ORDER BY COALESCE(SUM(oi.price_at_time * oi.quantity),0) DESC) AS rank\n" +
            "  FROM products p LEFT JOIN order_items oi ON oi.product_id = p.product_id\n" +
            "  GROUP BY p.seller_id\n" +
            ") r WHERE r.seller_id = :sellerId", nativeQuery = true)
    Integer sellerRevenueRank(@Param("sellerId") Long sellerId);

    // Total distinct sellers having at least one product
    @Query(value = "SELECT COUNT(DISTINCT seller_id) FROM products", nativeQuery = true)
        Long totalSellers();

        // Đếm số sản phẩm theo categoryId và status
        @Query("SELECT COUNT(p) FROM Products p WHERE LOWER(p.status) = LOWER(:status) AND EXISTS (SELECT 1 FROM CategoriesProducts cp WHERE cp.id.productId = p.productId AND cp.id.categoryId = :categoryId)")
        Long countByCategoryIdAndStatus(@Param("categoryId") Long categoryId, @Param("status") String status);
}
