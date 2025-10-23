package banhangrong.su25.Repository;

import banhangrong.su25.Entity.OrderItems;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface OrderItemsRepository extends JpaRepository<OrderItems, Long> {
    List<OrderItems> findByOrderId(Long orderId);
    
    @Query("SELECT oi FROM OrderItems oi JOIN Orders o ON oi.orderId = o.orderId WHERE o.userId = :userId")
    List<OrderItems> findByUserId(@Param("userId") Long userId);
    
    @Query("SELECT oi FROM OrderItems oi JOIN Orders o ON oi.orderId = o.orderId WHERE oi.orderItemId = :orderItemId AND o.userId = :userId")
    Optional<OrderItems> findByOrderItemIdAndUserId(@Param("orderItemId") Long orderItemId, @Param("userId") Long userId);
}