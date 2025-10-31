package banhangrong.su25.Repository;

import banhangrong.su25.Entity.Categories;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface AdminCategoryRepository extends JpaRepository <Categories, Long>{
    @Query("SELECT COUNT(c) FROM Categories c WHERE c.name = :name")
    public long countByName(String name);
}
