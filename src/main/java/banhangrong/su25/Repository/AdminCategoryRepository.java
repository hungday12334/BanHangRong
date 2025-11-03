package banhangrong.su25.Repository;

import banhangrong.su25.Entity.Categories;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AdminCategoryRepository extends JpaRepository <Categories, Long>{
    @Query("SELECT COUNT(c) FROM Categories c WHERE c.name = :name")
    public long countByName(String name);
    @Modifying
    @Transactional
    @Query(value = "DELETE FROM categories_products WHERE category_id = :id", nativeQuery = true)
    void deleteFromCategoriesProducts(@Param("id") Long id);

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM categories WHERE category_id = :id", nativeQuery = true)
    void deleteFromCategories(@Param("id") Long id);
}
