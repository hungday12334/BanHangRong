package banhangrong.su25.Repository;

import banhangrong.su25.Entity.Orders;
import org.springframework.data.jpa.repository.JpaRepository;
<<<<<<< Updated upstream

public interface OrdersRepository extends JpaRepository<Orders, Long> {
}
=======
import org.springframework.stereotype.Repository;

@Repository
public interface OrdersRepository extends JpaRepository<Orders, Long> {
}

>>>>>>> Stashed changes
