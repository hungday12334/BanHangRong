package banhangrong.su25.Repository;

import banhangrong.su25.Entity.Products;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AdminProductsRepository extends JpaRepository<Products, Long> {
    List<Products> findByStatus(String status);
    Long countByStatus(String status);

    // Case-insensitive helpers
    List<Products> findByStatusIgnoreCase(String status);
    Long countByStatusIgnoreCase(String status);

    List<Products> findBySellerIdAndStatusIgnoreCase(Long sellerId, String status);


}
