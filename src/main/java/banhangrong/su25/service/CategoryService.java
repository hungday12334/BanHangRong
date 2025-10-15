package banhangrong.su25.service;

import banhangrong.su25.Entity.Categories;
import banhangrong.su25.Repository.CategoriesRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class CategoryService {

    @Autowired
    private CategoriesRepository categoriesRepository;

    public List<Categories> getAllCategories() {
        try {
            return categoriesRepository.findAllByOrderByNameAsc();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Lỗi khi lấy danh sách categories: " + e.getMessage());
        }
    }

    public Categories createCategory(Categories category) {
        try {
            // Check if category name already exists
            if (categoriesRepository.existsByName(category.getName())) {
                throw new RuntimeException("Tên danh mục '" + category.getName() + "' đã tồn tại");
            }

            category.setCreatedAt(LocalDateTime.now());
            category.setUpdatedAt(LocalDateTime.now());
            return categoriesRepository.save(category);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Lỗi khi tạo category: " + e.getMessage());
        }
    }

    public Categories updateCategory(Long categoryId, Categories categoryDetails) {
        try {
            Categories category = categoriesRepository.findById(categoryId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy danh mục với ID: " + categoryId));

            // Check if new name conflicts with existing categories (excluding current)
            if (!category.getName().equals(categoryDetails.getName()) &&
                    categoriesRepository.existsByName(categoryDetails.getName())) {
                throw new RuntimeException("Tên danh mục '" + categoryDetails.getName() + "' đã tồn tại");
            }

            category.setName(categoryDetails.getName());
            category.setDescription(categoryDetails.getDescription());
            category.setUpdatedAt(LocalDateTime.now());

            return categoriesRepository.save(category);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Lỗi khi cập nhật category: " + e.getMessage());
        }
    }

    public void deleteCategory(Long categoryId) {
        try {
            Categories category = categoriesRepository.findById(categoryId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy danh mục với ID: " + categoryId));

            categoriesRepository.delete(category);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Lỗi khi xóa category: " + e.getMessage());
        }
    }

    public Optional<Categories> getCategoryById(Long categoryId) {
        return categoriesRepository.findById(categoryId);
    }
}