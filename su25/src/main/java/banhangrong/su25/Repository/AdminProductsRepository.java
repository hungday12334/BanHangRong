package banhangrong.su25.Repository;

import banhangrong.su25.Entity.Products;
import banhangrong.su25.Entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminProductsRepository extends JpaRepository<Products, Long> {
}
