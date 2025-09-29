package banhangrong.su25.Repository;

import banhangrong.su25.Entity.OrderItems;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderItemsRepository extends JpaRepository<OrderItems, Long> {
    List<OrderItems> findByOrderId(Long orderId);
}
