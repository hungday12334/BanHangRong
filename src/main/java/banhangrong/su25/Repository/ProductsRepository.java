package banhangrong.su25.Repository;

import banhangrong.su25.Entity.Products;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface ProductsRepository extends JpaRepository<Products, Long> {

    // ========================= SẢN PHẨM CHUNG =========================

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
    Page<Products> findByCategoryIdAndStatusAndSearch(@Param("categoryId") Long categoryId, @Param("status") String status, @Param("search") String search, Pageable pageable);

    // Tìm theo seller
    List<Products> findBySellerId(Long sellerId);

    // Find products by category regardless of status
    @Query("SELECT p FROM Products p WHERE EXISTS (SELECT 1 FROM CategoriesProducts cp WHERE cp.id.productId = p.productId AND cp.id.categoryId = :categoryId)")
    List<Products> findByCategoryIdAnyStatus(@Param("categoryId") Long categoryId);

    // Tìm top 10 sản phẩm sắp hết hàng
    @Query("SELECT p FROM Products p WHERE p.sellerId = :sellerId AND LOWER(p.status) = LOWER(:status) AND p.quantity <= :threshold ORDER BY p.quantity ASC")
    List<Products> findTop10BySellerIdAndStatusAndQuantityLessThanEqualOrderByQuantityAsc(@Param("sellerId") Long sellerId, @Param("status") String status, @Param("threshold") Integer threshold);

    // Đếm số sản phẩm theo seller và status
    @Query("SELECT COUNT(p) FROM Products p WHERE p.sellerId = :sellerId AND LOWER(p.status) = LOWER(:status)")
    long countBySellerIdAndStatus(@Param("sellerId") Long sellerId, @Param("status") String status);

    // Tổng doanh thu theo seller
    @Query(value = "SELECT COALESCE(SUM(oi.price_at_time * oi.quantity),0) FROM order_items oi JOIN products p ON p.product_id = oi.product_id WHERE p.seller_id = :sellerId", nativeQuery = true)
    BigDecimal totalRevenueBySeller(@Param("sellerId") Long sellerId);

    // Tổng sản phẩm bán được
    @Query(value = "SELECT COALESCE(SUM(oi.quantity),0) FROM order_items oi JOIN products p ON p.product_id = oi.product_id WHERE p.seller_id = :sellerId", nativeQuery = true)
    Long totalUnitsSoldBySeller(@Param("sellerId") Long sellerId);

    // Doanh thu theo ngày
    @Query(value = "SELECT CAST(oi.created_at AS DATE) as d, COALESCE(SUM(oi.price_at_time * oi.quantity),0) as revenue FROM order_items oi JOIN products p ON p.product_id = oi.product_id WHERE p.seller_id = :sellerId AND oi.created_at >= :fromDate GROUP BY CAST(oi.created_at AS DATE) ORDER BY CAST(oi.created_at AS DATE)", nativeQuery = true)
    List<Object[]> dailyRevenueFrom(@Param("sellerId") Long sellerId, @Param("fromDate") LocalDateTime fromDate);

    // Tổng số đơn hàng theo seller
    @Query(value = "SELECT COUNT(DISTINCT oi.order_id) FROM order_items oi JOIN products p ON p.product_id = oi.product_id WHERE p.seller_id = :sellerId", nativeQuery = true)
    Long totalOrdersBySeller(@Param("sellerId") Long sellerId);

    // Điểm trung bình sản phẩm theo seller
    @Query(value = "SELECT COALESCE(AVG(pr.rating),0) FROM product_reviews pr JOIN products p ON p.product_id = pr.product_id WHERE p.seller_id = :sellerId", nativeQuery = true)
    BigDecimal averageRatingBySeller(@Param("sellerId") Long sellerId);

    // Top 5 sản phẩm doanh thu cao nhất
    @Query(value = "SELECT p.product_id, p.name, COALESCE(SUM(oi.quantity),0) AS units, COALESCE(SUM(oi.quantity * oi.price_at_time),0) AS revenue, COALESCE(AVG(pr.rating),0) AS rating FROM products p LEFT JOIN order_items oi ON oi.product_id = p.product_id LEFT JOIN product_reviews pr ON pr.product_id = p.product_id WHERE p.seller_id = :sellerId GROUP BY p.product_id, p.name ORDER BY revenue DESC LIMIT 5", nativeQuery = true)
    List<Object[]> topProducts(@Param("sellerId") Long sellerId);

    // 8 đơn hàng gần nhất
    @Query(value = "SELECT o.order_id, o.created_at, COALESCE(SUM(oi.quantity * oi.price_at_time),0) AS amount, COALESCE(SUM(oi.quantity),0) AS items FROM orders o JOIN order_items oi ON oi.order_id = o.order_id JOIN products p ON p.product_id = oi.product_id WHERE p.seller_id = :sellerId GROUP BY o.order_id, o.created_at ORDER BY o.created_at DESC LIMIT 8", nativeQuery = true)
    List<Object[]> recentOrders(@Param("sellerId") Long sellerId);

    // Doanh thu hôm nay
    @Query(value = "SELECT COALESCE(SUM(oi.price_at_time * oi.quantity),0) FROM order_items oi JOIN products p ON p.product_id = oi.product_id WHERE p.seller_id = :sellerId AND CAST(oi.created_at AS DATE) = CURRENT_DATE", nativeQuery = true)
    BigDecimal todayRevenue(@Param("sellerId") Long sellerId);

    // Doanh thu tháng này
    @Query(value = "SELECT COALESCE(SUM(oi.price_at_time * oi.quantity),0) FROM order_items oi JOIN products p ON p.product_id = oi.product_id WHERE p.seller_id = :sellerId AND YEAR(oi.created_at) = YEAR(CURRENT_DATE) AND MONTH(oi.created_at) = MONTH(CURRENT_DATE)", nativeQuery = true)
    BigDecimal thisMonthRevenue(@Param("sellerId") Long sellerId);

    // Top 10 seller theo doanh thu
    @Query(value = "SELECT p.seller_id, u.username, COALESCE(SUM(oi.price_at_time * oi.quantity),0) AS revenue, COALESCE(SUM(oi.quantity),0) AS units FROM products p JOIN users u ON u.user_id = p.seller_id LEFT JOIN order_items oi ON oi.product_id = p.product_id GROUP BY p.seller_id, u.username ORDER BY revenue DESC LIMIT 10", nativeQuery = true)
    List<Object[]> topSellers();

    // Rank seller theo doanh thu
    @Query(value = "SELECT r.rank FROM (SELECT p.seller_id, DENSE_RANK() OVER (ORDER BY COALESCE(SUM(oi.price_at_time * oi.quantity),0) DESC) AS rank FROM products p LEFT JOIN order_items oi ON oi.product_id = p.product_id GROUP BY p.seller_id) r WHERE r.seller_id = :sellerId", nativeQuery = true)
    Integer sellerRevenueRank(@Param("sellerId") Long sellerId);

    // Tổng số seller
    @Query(value = "SELECT COUNT(DISTINCT seller_id) FROM products", nativeQuery = true)
    Long totalSellers();

    // Đếm sản phẩm theo categoryId và status
    @Query("SELECT COUNT(p) FROM Products p WHERE LOWER(p.status) = LOWER(:status) AND EXISTS (SELECT 1 FROM CategoriesProducts cp WHERE cp.id.productId = p.productId AND cp.id.categoryId = :categoryId)")
    Long countByCategoryIdAndStatus(@Param("categoryId") Long categoryId, @Param("status") String status);


    // ========================= CÁC HÀM MỚI CHO SHOP DESIGN =========================

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
