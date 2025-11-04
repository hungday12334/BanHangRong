package banhangrong.su25.Repository;

import banhangrong.su25.Entity.ShoppingCart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface ShoppingCartRepository extends JpaRepository<ShoppingCart, Long> {
    List<ShoppingCart> findByUserId(Long userId);
    void deleteByUserIdAndProductId(Long userId, Long productId);
    long countByUserId(Long userId);
    Optional<ShoppingCart> findByUserIdAndProductId(Long userId, Long productId);

    @Query("SELECT COALESCE(SUM(CASE WHEN sc.quantity IS NULL THEN 0 ELSE sc.quantity END), 0) FROM ShoppingCart sc WHERE sc.userId = :userId")
    Long sumQuantityByUserId(@Param("userId") Long userId);
}


