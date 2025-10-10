package banhangrong.su25.Repository;

import banhangrong.su25.Entity.Products;
import banhangrong.su25.Entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AdminProductsRepository extends JpaRepository<Products, Long> {
    public List<Products> findByStatus(String status);
    public Long countByStatus(String status);
}
