package banhangrong.su25.Repository;

import banhangrong.su25.Entity.Categories;
import banhangrong.su25.Entity.CategoriesProducts;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategoriesProductsRepository extends JpaRepository<CategoriesProducts, Long> {

    @Query("SELECT c FROM Categories c WHERE c.categoryId IN (SELECT cp.id.categoryId FROM CategoriesProducts cp WHERE cp.id.productId = :productId)")
    List<Categories> findCategoriesByProductId(@Param("productId") Long productId);

    @Query("SELECT COUNT(cp) > 0 FROM CategoriesProducts cp WHERE cp.id.categoryId = :categoryId")
    boolean existsByCategoryId(@Param("categoryId") Long categoryId);
}
