package banhangrong.su25.service;

import banhangrong.su25.DTO.CategoryFilter;
import banhangrong.su25.Entity.Categories;

import java.util.List;

public interface AdminCategoryService {
    public List<Categories> findAll();
    public Categories findById(Long id);
    public Categories save(Categories category);
    public void delete(Long id);
    public void update(Categories category);
    public List<Categories> filter(CategoryFilter filter);
}
