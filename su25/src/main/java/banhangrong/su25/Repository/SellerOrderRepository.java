package banhangrong.su25.Repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Lightweight repository exposing aggregated order view for a specific seller (multi-vendor aware).
 */
public interface SellerOrderRepository extends Repository<banhangrong.su25.Entity.Orders, Long> {

    interface SellerOrderSummary {
        Long getOrderId();
        LocalDateTime getCreatedAt();
        BigDecimal getSellerAmount();
        Long getSellerItems();
        Long getUserId();
        String getUsername();
    }

    @Query(value = """
            SELECT o.order_id       AS orderId,
                   o.created_at     AS createdAt,
                   u.user_id        AS userId,
                   u.username       AS username,
                   SUM(CASE WHEN p.seller_id = :sellerId THEN (oi.quantity * oi.price_at_time) ELSE 0 END) AS sellerAmount,
                   SUM(CASE WHEN p.seller_id = :sellerId THEN oi.quantity ELSE 0 END)                    AS sellerItems
            FROM orders o
            JOIN order_items oi ON o.order_id = oi.order_id
            JOIN products p ON oi.product_id = p.product_id
            LEFT JOIN users u ON o.user_id = u.user_id
            WHERE p.seller_id = :sellerId
              AND (:fromTs IS NULL OR o.created_at >= :fromTs)
              AND (:toTs IS NULL OR o.created_at <= :toTs)
              AND (
                   :search IS NULL OR :search = '' OR
                   CAST(o.order_id AS CHAR) LIKE CONCAT('%', :search, '%') OR
                   (u.username IS NOT NULL AND u.username LIKE CONCAT('%', :search, '%'))
              )
            GROUP BY o.order_id, o.created_at, u.user_id, u.username
            HAVING SUM(CASE WHEN p.seller_id = :sellerId THEN oi.quantity ELSE 0 END) > 0
            ORDER BY o.created_at DESC, o.order_id DESC
            """,
            countQuery = """
            SELECT COUNT(*) FROM (
                SELECT o.order_id
                FROM orders o
                JOIN order_items oi ON o.order_id = oi.order_id
                JOIN products p ON oi.product_id = p.product_id
                LEFT JOIN users u ON o.user_id = u.user_id
                WHERE p.seller_id = :sellerId
                  AND (:fromTs IS NULL OR o.created_at >= :fromTs)
                  AND (:toTs IS NULL OR o.created_at <= :toTs)
                  AND (
                       :search IS NULL OR :search = '' OR
                       CAST(o.order_id AS CHAR) LIKE CONCAT('%', :search, '%') OR
                       (u.username IS NOT NULL AND u.username LIKE CONCAT('%', :search, '%'))
                  )
                GROUP BY o.order_id
            ) tmp
            """,
            nativeQuery = true)
    Page<SellerOrderSummary> findSellerOrders(@Param("sellerId") Long sellerId,
                                              @Param("fromTs") LocalDateTime fromTs,
                                              @Param("toTs") LocalDateTime toTs,
                                              @Param("search") String search,
                                              Pageable pageable);
}
