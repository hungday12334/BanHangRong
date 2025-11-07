package banhangrong.su25.Repository;

import banhangrong.su25.Entity.Orders;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;

public interface OrdersRepository extends JpaRepository<Orders, Long> {
    Page<Orders> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    
    Page<Orders> findByUserIdOrderByCreatedAtAsc(Long userId, Pageable pageable);
    
    Page<Orders> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, String status, Pageable pageable);
    
    Page<Orders> findByUserIdAndStatusOrderByCreatedAtAsc(Long userId, String status, Pageable pageable);
    
    // Date filter methods - DESC
    @Query("SELECT o FROM Orders o WHERE o.userId = :userId AND o.createdAt >= :startDate ORDER BY o.createdAt DESC")
    Page<Orders> findByUserIdAndCreatedAtAfterOrderByCreatedAtDesc(@Param("userId") Long userId, 
                                                                    @Param("startDate") LocalDateTime startDate, 
                                                                    Pageable pageable);
    
    @Query("SELECT o FROM Orders o WHERE o.userId = :userId AND o.createdAt >= :startDate ORDER BY o.createdAt ASC")
    Page<Orders> findByUserIdAndCreatedAtAfterOrderByCreatedAtAsc(@Param("userId") Long userId, 
                                                                   @Param("startDate") LocalDateTime startDate, 
                                                                   Pageable pageable);
    
    @Query("SELECT o FROM Orders o WHERE o.userId = :userId AND o.status = :status AND o.createdAt >= :startDate ORDER BY o.createdAt DESC")
    Page<Orders> findByUserIdAndStatusAndCreatedAtAfterOrderByCreatedAtDesc(@Param("userId") Long userId, 
                                                                             @Param("status") String status, 
                                                                             @Param("startDate") LocalDateTime startDate, 
                                                                             Pageable pageable);
    
    @Query("SELECT o FROM Orders o WHERE o.userId = :userId AND o.status = :status AND o.createdAt >= :startDate ORDER BY o.createdAt ASC")
    Page<Orders> findByUserIdAndStatusAndCreatedAtAfterOrderByCreatedAtAsc(@Param("userId") Long userId, 
                                                                           @Param("status") String status, 
                                                                           @Param("startDate") LocalDateTime startDate, 
                                                                           Pageable pageable);
    
    @Query("SELECT DISTINCT o FROM Orders o " +
           "JOIN OrderItems oi ON o.orderId = oi.orderId " +
           "JOIN Products p ON oi.productId = p.productId " +
           "WHERE o.userId = :userId " +
           "AND o.createdAt >= :startDate " +
           "AND (LOWER(p.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "OR LOWER(CAST(o.sellerId AS string)) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) " +
           "ORDER BY o.createdAt DESC")
    Page<Orders> findByUserIdAndSearchTermAndCreatedAtAfter(@Param("userId") Long userId, 
                                                            @Param("searchTerm") String searchTerm, 
                                                            @Param("startDate") LocalDateTime startDate, 
                                                            Pageable pageable);
    
    @Query("SELECT DISTINCT o FROM Orders o " +
           "JOIN OrderItems oi ON o.orderId = oi.orderId " +
           "JOIN Products p ON oi.productId = p.productId " +
           "WHERE o.userId = :userId " +
           "AND o.createdAt >= :startDate " +
           "AND (LOWER(p.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "OR LOWER(CAST(o.sellerId AS string)) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) " +
           "ORDER BY o.createdAt ASC")
    Page<Orders> findByUserIdAndSearchTermAndCreatedAtAfterAsc(@Param("userId") Long userId, 
                                                                @Param("searchTerm") String searchTerm, 
                                                                @Param("startDate") LocalDateTime startDate, 
                                                                Pageable pageable);
    
    @Query("SELECT DISTINCT o FROM Orders o " +
           "JOIN OrderItems oi ON o.orderId = oi.orderId " +
           "JOIN Products p ON oi.productId = p.productId " +
           "WHERE o.userId = :userId " +
           "AND (LOWER(p.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "OR LOWER(CAST(o.sellerId AS string)) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) " +
           "ORDER BY o.createdAt DESC")
    Page<Orders> findByUserIdAndSearchTerm(@Param("userId") Long userId, 
                                          @Param("searchTerm") String searchTerm, 
                                          Pageable pageable);
    
    @Query("SELECT DISTINCT o FROM Orders o " +
           "JOIN OrderItems oi ON o.orderId = oi.orderId " +
           "JOIN Products p ON oi.productId = p.productId " +
           "WHERE o.userId = :userId " +
           "AND (LOWER(p.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "OR LOWER(CAST(o.sellerId AS string)) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) " +
           "ORDER BY o.createdAt ASC")
    Page<Orders> findByUserIdAndSearchTermAsc(@Param("userId") Long userId, 
                                             @Param("searchTerm") String searchTerm, 
                                             Pageable pageable);
    
    // Filter by license key status - Active (has at least one active license)
    @Query("SELECT DISTINCT o FROM Orders o " +
           "JOIN OrderItems oi ON o.orderId = oi.orderId " +
           "JOIN ProductLicenses pl ON oi.orderItemId = pl.orderItemId " +
           "WHERE o.userId = :userId AND pl.isActive = true " +
           "ORDER BY o.createdAt DESC")
    Page<Orders> findByUserIdAndLicenseActiveOrderByCreatedAtDesc(@Param("userId") Long userId, Pageable pageable);
    
    @Query("SELECT DISTINCT o FROM Orders o " +
           "JOIN OrderItems oi ON o.orderId = oi.orderId " +
           "JOIN ProductLicenses pl ON oi.orderItemId = pl.orderItemId " +
           "WHERE o.userId = :userId AND pl.isActive = true " +
           "ORDER BY o.createdAt ASC")
    Page<Orders> findByUserIdAndLicenseActiveOrderByCreatedAtAsc(@Param("userId") Long userId, Pageable pageable);
    
    // Filter by license key status - Expired (all licenses inactive or no licenses)
    @Query("SELECT o FROM Orders o " +
           "WHERE o.userId = :userId " +
           "AND NOT EXISTS (" +
           "  SELECT 1 FROM ProductLicenses pl " +
           "  JOIN OrderItems oi ON pl.orderItemId = oi.orderItemId " +
           "  WHERE oi.orderId = o.orderId AND pl.isActive = true" +
           ") " +
           "ORDER BY o.createdAt DESC")
    Page<Orders> findByUserIdAndLicenseExpiredOrderByCreatedAtDesc(@Param("userId") Long userId, Pageable pageable);
    
    @Query("SELECT o FROM Orders o " +
           "WHERE o.userId = :userId " +
           "AND NOT EXISTS (" +
           "  SELECT 1 FROM ProductLicenses pl " +
           "  JOIN OrderItems oi ON pl.orderItemId = oi.orderItemId " +
           "  WHERE oi.orderId = o.orderId AND pl.isActive = true" +
           ") " +
           "ORDER BY o.createdAt ASC")
    Page<Orders> findByUserIdAndLicenseExpiredOrderByCreatedAtAsc(@Param("userId") Long userId, Pageable pageable);
    
    // With date filter - Active
    @Query("SELECT DISTINCT o FROM Orders o " +
           "JOIN OrderItems oi ON o.orderId = oi.orderId " +
           "JOIN ProductLicenses pl ON oi.orderItemId = pl.orderItemId " +
           "WHERE o.userId = :userId AND pl.isActive = true AND o.createdAt >= :startDate " +
           "ORDER BY o.createdAt DESC")
    Page<Orders> findByUserIdAndLicenseActiveAndCreatedAtAfterOrderByCreatedAtDesc(@Param("userId") Long userId, 
                                                                                  @Param("startDate") LocalDateTime startDate, 
                                                                                  Pageable pageable);
    
    @Query("SELECT DISTINCT o FROM Orders o " +
           "JOIN OrderItems oi ON o.orderId = oi.orderId " +
           "JOIN ProductLicenses pl ON oi.orderItemId = pl.orderItemId " +
           "WHERE o.userId = :userId AND pl.isActive = true AND o.createdAt >= :startDate " +
           "ORDER BY o.createdAt ASC")
    Page<Orders> findByUserIdAndLicenseActiveAndCreatedAtAfterOrderByCreatedAtAsc(@Param("userId") Long userId, 
                                                                                    @Param("startDate") LocalDateTime startDate, 
                                                                                    Pageable pageable);
    
    // With date filter - Expired
    @Query("SELECT o FROM Orders o " +
           "WHERE o.userId = :userId AND o.createdAt >= :startDate " +
           "AND NOT EXISTS (" +
           "  SELECT 1 FROM ProductLicenses pl " +
           "  JOIN OrderItems oi ON pl.orderItemId = oi.orderItemId " +
           "  WHERE oi.orderId = o.orderId AND pl.isActive = true" +
           ") " +
           "ORDER BY o.createdAt DESC")
    Page<Orders> findByUserIdAndLicenseExpiredAndCreatedAtAfterOrderByCreatedAtDesc(@Param("userId") Long userId, 
                                                                                      @Param("startDate") LocalDateTime startDate, 
                                                                                      Pageable pageable);
    
    @Query("SELECT o FROM Orders o " +
           "WHERE o.userId = :userId AND o.createdAt >= :startDate " +
           "AND NOT EXISTS (" +
           "  SELECT 1 FROM ProductLicenses pl " +
           "  JOIN OrderItems oi ON pl.orderItemId = oi.orderItemId " +
           "  WHERE oi.orderId = o.orderId AND pl.isActive = true" +
           ") " +
           "ORDER BY o.createdAt ASC")
    Page<Orders> findByUserIdAndLicenseExpiredAndCreatedAtAfterOrderByCreatedAtAsc(@Param("userId") Long userId, 
                                                                                    @Param("startDate") LocalDateTime startDate, 
                                                                                    Pageable pageable);
}
