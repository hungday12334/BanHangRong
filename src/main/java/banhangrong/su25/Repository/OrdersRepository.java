package banhangrong.su25.Repository;

import banhangrong.su25.Entity.Orders;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrdersRepository extends JpaRepository<Orders, Long> {
    Page<Orders> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    
    Page<Orders> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, String status, Pageable pageable);
    
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
}
