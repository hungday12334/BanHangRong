package banhangrong.su25.Repository;

import banhangrong.su25.Entity.ShoppingCart;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ShoppingCartRepository extends JpaRepository<ShoppingCart, Long> {
    List<ShoppingCart> findByUserId(Long userId);
    void deleteByUserIdAndProductId(Long userId, Long productId);
    long countByUserId(Long userId);
    Optional<ShoppingCart> findByUserIdAndProductId(Long userId, Long productId);
}


