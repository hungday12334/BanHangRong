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
    
    // Tìm category theo tên
    Optional<Categories> findByName(String name);
    
    // Tìm tất cả categories, sắp xếp theo tên
    List<Categories> findAllByOrderByNameAsc();
    
    // Tìm categories có chứa từ khóa trong tên (case insensitive)
    List<Categories> findByNameContainingIgnoreCase(String name);
    
    // Đếm số lượng products trong một category
    @Query("SELECT COUNT(p) FROM Products p JOIN p.categories c WHERE c.categoryId = :categoryId AND p.status = 'Public'")
    Long countProductsByCategoryId(@Param("categoryId") Long categoryId);
    
    // Tìm categories có ít nhất một product public
    @Query("SELECT DISTINCT c FROM Categories c JOIN c.products p WHERE p.status = 'Public' ORDER BY c.name ASC")
    List<Categories> findCategoriesWithPublicProducts();
}
