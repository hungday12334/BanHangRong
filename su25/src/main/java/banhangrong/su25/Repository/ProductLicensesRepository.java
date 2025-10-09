package banhangrong.su25.Repository;

import banhangrong.su25.Entity.ProductLicenses;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface ProductLicensesRepository extends JpaRepository<ProductLicenses, Long> {

    /**
     * View model for listing licenses belonging to a seller (via product -> seller relation).
     */
    interface LicenseView {
        Long getLicenseId();
        String getLicenseKey();
        Boolean getIsActive();
        LocalDateTime getActivationDate();
        LocalDateTime getLastUsedDate();
        String getDeviceIdentifier();
        LocalDateTime getCreatedAt();
        Long getOrderItemId();
        Long getOrderId();
        Long getProductId();
        String getProductName();
    }

    @Query(value = """
            /*
             * Logic:
             *  - A license is associated to a seller if its order_item maps to a product of that seller.
             *  - Future extension: pre-generated licenses not yet tied to order_item_id (NULL) could be linked by a direct product_id column; hiện chưa có nên sẽ bỏ qua.
             */
            SELECT l.license_id AS licenseId,
                   l.license_key AS licenseKey,
                   l.is_active AS isActive,
                   l.activation_date AS activationDate,
                   l.last_used_date AS lastUsedDate,
                   l.device_identifier AS deviceIdentifier,
                   l.created_at AS createdAt,
                   l.order_item_id AS orderItemId,
                   oi.order_id AS orderId,
                   p.product_id AS productId,
                   p.name AS productName
            FROM product_licenses l
            LEFT JOIN order_items oi ON l.order_item_id = oi.order_item_id
            LEFT JOIN products p ON oi.product_id = p.product_id
            WHERE (p.seller_id = :sellerId)
              AND (:active IS NULL OR l.is_active = :active)
              AND (:productId IS NULL OR p.product_id = :productId)
              AND (:search IS NULL OR l.license_key LIKE CONCAT('%', :search, '%'))
            ORDER BY l.created_at DESC, l.license_id DESC
            """,
            countQuery = """
            SELECT COUNT(*) FROM (
              SELECT l.license_id
              FROM product_licenses l
              LEFT JOIN order_items oi ON l.order_item_id = oi.order_item_id
              LEFT JOIN products p ON oi.product_id = p.product_id
              WHERE (p.seller_id = :sellerId)
                AND (:active IS NULL OR l.is_active = :active)
                AND (:productId IS NULL OR p.product_id = :productId)
                AND (:search IS NULL OR l.license_key LIKE CONCAT('%', :search, '%'))
            ) x
            """,
            nativeQuery = true)
    Page<LicenseView> findSellerLicenses(@Param("sellerId") Long sellerId,
                                         @Param("active") Boolean active,
                                         @Param("productId") Long productId,
                                         @Param("search") String search,
                                         Pageable pageable);
}
