package banhangrong.su25.Repository;

import banhangrong.su25.Entity.Categories;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface CategoriesRepository extends JpaRepository<Categories, Long> {

    // Tìm tất cả categories sắp xếp theo tên
    List<Categories> findAllByOrderByNameAsc();

    // Tìm category theo tên
    Optional<Categories> findByName(String name);

    // Kiểm tra category name đã tồn tại chưa
    boolean existsByName(String name);

    // Kiểm tra category name đã tồn tại chưa (không phân biệt hoa thường)
    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM Categories c WHERE LOWER(c.name) = LOWER(:name)")
    boolean existsByNameIgnoreCase(@Param("name") String name);

    // Hoặc dùng query tùy chỉnh
    @Query("SELECT c FROM Categories c ORDER BY c.name ASC")
    List<Categories> findAllOrderByNameAsc();

    // Tìm categories có ít nhất một product public
    @Query("SELECT c FROM Categories c WHERE EXISTS (SELECT 1 FROM Products p WHERE LOWER(p.status) = LOWER('Public') AND EXISTS (SELECT 1 FROM CategoriesProducts cp WHERE cp.id.productId = p.productId AND cp.id.categoryId = c.categoryId)) ORDER BY c.name ASC")
    List<Categories> findCategoriesWithPublicProducts();
}