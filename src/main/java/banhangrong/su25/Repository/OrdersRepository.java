package banhangrong.su25.Repository;

import banhangrong.su25.Entity.Orders;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;

public interface OrdersRepository extends JpaRepository<Orders, Long> {
	// Reusable placeholder order for pre-generated licenses: user_id = sellerId, total_amount = 0
	Orders findTopByUserIdAndTotalAmountOrderByCreatedAtDesc(Long userId, BigDecimal totalAmount);

	// Customer dashboard: list orders by user with newest first, paginated
	Page<Orders> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
}
