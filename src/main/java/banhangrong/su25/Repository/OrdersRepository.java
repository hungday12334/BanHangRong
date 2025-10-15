package banhangrong.su25.Repository;

import banhangrong.su25.Entity.Orders;
import org.springframework.data.jpa.repository.JpaRepository;

import java.math.BigDecimal;

public interface OrdersRepository extends JpaRepository<Orders, Long> {
	// Reusable placeholder order for pre-generated licenses: user_id = sellerId, total_amount = 0
	Orders findTopByUserIdAndTotalAmountOrderByCreatedAtDesc(Long userId, BigDecimal totalAmount);
}
