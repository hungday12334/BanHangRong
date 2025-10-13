package banhangrong.su25.Repository;

import banhangrong.su25.Entity.Categories;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
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

    // Hoặc dùng query tùy chỉnh
    @Query("SELECT c FROM Categories c ORDER BY c.name ASC")
    List<Categories> findAllOrderByNameAsc();
}