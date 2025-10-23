package banhangrong.su25.Repository;

import banhangrong.su25.Entity.Categories;
import banhangrong.su25.Entity.CategoriesProducts;
import banhangrong.su25.Entity.CategoriesProductsId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategoriesProductsRepository extends JpaRepository<CategoriesProducts, CategoriesProductsId> {

    @Query("SELECT cp.category FROM CategoriesProducts cp WHERE cp.product.productId = :productId")
    List<Categories> findCategoriesByProductId(@Param("productId") Long productId);

    @Query("SELECT COUNT(cp) > 0 FROM CategoriesProducts cp WHERE cp.category.categoryId = :categoryId")
    boolean existsByCategoryId(@Param("categoryId") Long categoryId);
}