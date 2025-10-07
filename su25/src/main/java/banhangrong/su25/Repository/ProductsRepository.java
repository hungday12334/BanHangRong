<<<<<<< Updated upstream
package banhangrong.su25.Repository;
=======
       package banhangrong.su25.Repository;
>>>>>>> Stashed changes

import banhangrong.su25.Entity.Products;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
<<<<<<< Updated upstream
=======
import org.springframework.stereotype.Repository;
>>>>>>> Stashed changes

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

<<<<<<< Updated upstream
public interface ProductsRepository extends JpaRepository<Products, Long> {
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

    @Query(value = "SELECT DATE(oi.created_at) as d, COALESCE(SUM(oi.price_at_time * oi.quantity),0) as revenue\n" +
            "FROM order_items oi JOIN products p ON p.product_id = oi.product_id\n" +
            "WHERE p.seller_id = :sellerId AND oi.created_at >= :fromDate\n" +
            "GROUP BY DATE(oi.created_at)\n" +
            "ORDER BY DATE(oi.created_at)", nativeQuery = true)
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
            "WHERE p.seller_id = :sellerId AND DATE(oi.created_at) = CURRENT_DATE", nativeQuery = true)
    BigDecimal todayRevenue(@Param("sellerId") Long sellerId);

    @Query(value = "SELECT COALESCE(SUM(oi.price_at_time * oi.quantity),0)\n" +
            "FROM order_items oi JOIN products p ON p.product_id = oi.product_id\n" +
            "WHERE p.seller_id = :sellerId AND YEAR(oi.created_at) = YEAR(CURRENT_DATE) AND MONTH(oi.created_at) = MONTH(CURRENT_DATE)", nativeQuery = true)
    BigDecimal thisMonthRevenue(@Param("sellerId") Long sellerId);
=======
@Repository
public interface ProductsRepository extends JpaRepository<Products, Long> {
    List<Products> findAllById(Iterable<Long> ids);
    
    // Custom queries for seller dashboard - Fixed for H2 compatibility
    @Query("SELECT COALESCE(SUM(oi.priceAtTime * oi.quantity), 0) FROM OrderItems oi " +
           "JOIN Orders o ON oi.orderId = o.orderId " +
           "JOIN Products p ON oi.productId = p.productId " +
           "WHERE p.sellerId = :sellerId")
    BigDecimal totalRevenueBySeller(@Param("sellerId") Long sellerId);
    
    @Query("SELECT COALESCE(SUM(oi.quantity), 0) FROM OrderItems oi " +
           "JOIN Products p ON oi.productId = p.productId " +
           "WHERE p.sellerId = :sellerId")
    Long totalUnitsSoldBySeller(@Param("sellerId") Long sellerId);
    
    @Query("SELECT COUNT(DISTINCT o.orderId) FROM Orders o " +
           "JOIN OrderItems oi ON o.orderId = oi.orderId " +
           "JOIN Products p ON oi.productId = p.productId " +
           "WHERE p.sellerId = :sellerId")
    Long totalOrdersBySeller(@Param("sellerId") Long sellerId);
    
    @Query("SELECT COALESCE(AVG(pr.rating), 0) FROM ProductReviews pr " +
           "JOIN Products p ON pr.productId = p.productId " +
           "WHERE p.sellerId = :sellerId")
    BigDecimal averageRatingBySeller(@Param("sellerId") Long sellerId);
    
    @Query("SELECT COALESCE(SUM(oi.priceAtTime * oi.quantity), 0) FROM OrderItems oi " +
           "JOIN Orders o ON oi.orderId = o.orderId " +
           "JOIN Products p ON oi.productId = p.productId " +
           "WHERE p.sellerId = :sellerId AND CAST(o.createdAt AS date) = CURRENT_DATE")
    BigDecimal todayRevenue(@Param("sellerId") Long sellerId);
    
    @Query("SELECT COALESCE(SUM(oi.priceAtTime * oi.quantity), 0) FROM OrderItems oi " +
           "JOIN Orders o ON oi.orderId = o.orderId " +
           "JOIN Products p ON oi.productId = p.productId " +
           "WHERE p.sellerId = :sellerId AND YEAR(o.createdAt) = YEAR(CURRENT_DATE) AND MONTH(o.createdAt) = MONTH(CURRENT_DATE)")
    BigDecimal thisMonthRevenue(@Param("sellerId") Long sellerId);
    
    @Query("SELECT CAST(o.createdAt AS date) as date, COALESCE(SUM(oi.priceAtTime * oi.quantity), 0) as revenue " +
           "FROM OrderItems oi " +
           "JOIN Orders o ON oi.orderId = o.orderId " +
           "JOIN Products p ON oi.productId = p.productId " +
           "WHERE p.sellerId = :sellerId AND o.createdAt >= :from " +
           "GROUP BY CAST(o.createdAt AS date) " +
           "ORDER BY CAST(o.createdAt AS date)")
    List<Object[]> dailyRevenueFrom(@Param("sellerId") Long sellerId, @Param("from") LocalDateTime from);
    
    @Query("SELECT p.productId, p.name, COALESCE(SUM(oi.quantity), 0) as units, " +
           "COALESCE(SUM(oi.priceAtTime * oi.quantity), 0) as revenue, " +
           "COALESCE(AVG(pr.rating), 0) as rating " +
           "FROM Products p " +
           "LEFT JOIN OrderItems oi ON p.productId = oi.productId " +
           "LEFT JOIN ProductReviews pr ON p.productId = pr.productId " +
           "WHERE p.sellerId = :sellerId " +
           "GROUP BY p.productId, p.name " +
           "ORDER BY units DESC")
    List<Object[]> topProducts(@Param("sellerId") Long sellerId);
    
    @Query("SELECT o.orderId, o.createdAt, o.totalAmount, COUNT(oi.orderItemId) as items " +
           "FROM Orders o " +
           "JOIN OrderItems oi ON o.orderId = oi.orderId " +
           "JOIN Products p ON oi.productId = p.productId " +
           "WHERE p.sellerId = :sellerId " +
           "GROUP BY o.orderId, o.createdAt, o.totalAmount " +
           "ORDER BY o.createdAt DESC")
    List<Object[]> recentOrders(@Param("sellerId") Long sellerId);
    
    List<Products> findTop10BySellerIdAndIsActiveTrueAndQuantityLessThanEqualOrderByQuantityAsc(Long sellerId, int quantity);
    
    long countBySellerIdAndIsActiveTrue(Long sellerId);
>>>>>>> Stashed changes
}
