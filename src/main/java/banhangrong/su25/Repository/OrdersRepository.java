package banhangrong.su25.Repository;

import banhangrong.su25.Entity.Orders;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrdersRepository extends JpaRepository<Orders, Long> {
    Page<Orders> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
}
