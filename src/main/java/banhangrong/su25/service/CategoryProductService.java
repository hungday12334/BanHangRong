package banhangrong.su25.service;

import banhangrong.su25.Repository.CategoriesProductsRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class CategoryProductService {
    private final CategoriesProductsRepository categoriesProductsRepository;
    public CategoryProductService(CategoriesProductsRepository categoriesProductsRepository) {
        this.categoriesProductsRepository = categoriesProductsRepository;
    }

}
