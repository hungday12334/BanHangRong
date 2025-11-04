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
            // Trim and validate name
            if (category.getName() != null) {
                category.setName(category.getName().trim());
            }

            // Check if name is empty or null after trimming
            if (category.getName() == null || category.getName().isEmpty()) {
                throw new RuntimeException("Tên danh mục không được để trống");
            }

            // Check length
            if (category.getName().length() < 2) {
                throw new RuntimeException("Tên danh mục phải có ít nhất 2 ký tự");
            }

            if (category.getName().length() > 100) {
                throw new RuntimeException("Tên danh mục không được vượt quá 100 ký tự");
            }

            // Check if category name already exists (case-insensitive)
            if (categoriesRepository.existsByNameIgnoreCase(category.getName())) {
                throw new RuntimeException("Tên danh mục '" + category.getName() + "' đã tồn tại");
            }

            // Trim description
            if (category.getDescription() != null) {
                category.setDescription(category.getDescription().trim());
                if (category.getDescription().isEmpty()) {
                    category.setDescription(null);
                }
                // Check description length
                if (category.getDescription() != null && category.getDescription().length() > 255) {
                    throw new RuntimeException("Mô tả không được vượt quá 255 ký tự");
                }
            }

            category.setCreatedAt(LocalDateTime.now());
            category.setUpdatedAt(LocalDateTime.now());
            return categoriesRepository.save(category);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Lỗi khi tạo category: " + e.getMessage());
        }
    }

    public Categories updateCategory(Long categoryId, Categories categoryDetails) {
        try {
            Categories category = categoriesRepository.findById(categoryId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy danh mục với ID: " + categoryId));

            // Trim and validate name
            if (categoryDetails.getName() != null) {
                categoryDetails.setName(categoryDetails.getName().trim());
            }

            // Check if name is empty after trimming
            if (categoryDetails.getName() == null || categoryDetails.getName().isEmpty()) {
                throw new RuntimeException("Tên danh mục không được để trống");
            }

            // Check length
            if (categoryDetails.getName().length() < 2) {
                throw new RuntimeException("Tên danh mục phải có ít nhất 2 ký tự");
            }

            if (categoryDetails.getName().length() > 100) {
                throw new RuntimeException("Tên danh mục không được vượt quá 100 ký tự");
            }

            // Check if new name conflicts with existing categories (excluding current, case-insensitive)
            if (!category.getName().equalsIgnoreCase(categoryDetails.getName())) {
                if (categoriesRepository.existsByNameIgnoreCase(categoryDetails.getName())) {
                    throw new RuntimeException("Tên danh mục '" + categoryDetails.getName() + "' đã tồn tại");
                }
            }

            category.setName(categoryDetails.getName());

            // Trim description
            if (categoryDetails.getDescription() != null) {
                categoryDetails.setDescription(categoryDetails.getDescription().trim());
                if (categoryDetails.getDescription().isEmpty()) {
                    categoryDetails.setDescription(null);
                }
                // Check description length
                if (categoryDetails.getDescription() != null && categoryDetails.getDescription().length() > 255) {
                    throw new RuntimeException("Mô tả không được vượt quá 255 ký tự");
                }
            }

            category.setDescription(categoryDetails.getDescription());
            category.setUpdatedAt(LocalDateTime.now());

            return categoriesRepository.save(category);
        } catch (RuntimeException e) {
            throw e;
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
        } catch (RuntimeException e) {
            // Re-throw runtime exceptions (including not found)
            throw e;
        } catch (Exception e) {
            // Handle foreign key constraint or other database errors
            String errorMsg = e.getMessage();
            if (errorMsg != null && (errorMsg.contains("foreign key") || errorMsg.contains("constraint"))) {
                throw new RuntimeException("Không thể xóa danh mục đang có sản phẩm. Vui lòng xóa hoặc chuyển các sản phẩm sang danh mục khác trước.");
            }
            e.printStackTrace();
            throw new RuntimeException("Lỗi khi xóa category: " + errorMsg);
        }
    }

    public Optional<Categories> getCategoryById(Long categoryId) {
        return categoriesRepository.findById(categoryId);
    }
}