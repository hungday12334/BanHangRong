package banhangrong.su25.service;

import banhangrong.su25.Entity.Categories;
import banhangrong.su25.Entity.ProductImages;
import banhangrong.su25.Entity.Products;
import banhangrong.su25.Entity.Users;
import banhangrong.su25.Repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CategoryViewService {

    private final CategoriesRepository categoriesRepository;
    private final ProductsRepository productsRepository;
    private final ProductImagesRepository productImagesRepository;
    private final ShoppingCartRepository shoppingCartRepository;
    private final UsersRepository usersRepository;

    public CategoryViewService(CategoriesRepository categoriesRepository,
                               ProductsRepository productsRepository,
                               ProductImagesRepository productImagesRepository,
                               ShoppingCartRepository shoppingCartRepository,
                               UsersRepository usersRepository) {
        this.categoriesRepository = categoriesRepository;
        this.productsRepository = productsRepository;
        this.productImagesRepository = productImagesRepository;
        this.shoppingCartRepository = shoppingCartRepository;
        this.usersRepository = usersRepository;
    }

    public Users getCurrentUserOrNull() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) return null;
        return usersRepository.findByUsername(auth.getName()).orElse(null);
    }

    public boolean shouldRedirectVerifyForCustomer() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
            Users currentUser = usersRepository.findByUsername(auth.getName()).orElse(null);
            if (currentUser != null && "CUSTOMER".equals(currentUser.getUserType())) {
                return !Boolean.TRUE.equals(currentUser.getIsEmailVerified());
            }
        }
        return false;
    }

    public List<Categories> listCategoriesWithPublicProducts() {
        return categoriesRepository.findCategoriesWithPublicProducts();
    }

    public Categories getCategoryById(Long categoryId) {
        if (categoryId == null) return null;
        return categoriesRepository.findById(categoryId).orElse(null);
    }

    public long countPublicProductsInCategory(Long categoryId) {
        return productsRepository.countByCategoryIdAndStatus(categoryId, "Public");
    }

    public Map<String, Long> countPublicProductsInCategories(List<Categories> categories) {
        Map<String, Long> result = new HashMap<>();
        for (Categories c : categories) {
            result.put(c.getName(), productsRepository.countByCategoryIdAndStatus(c.getCategoryId(), "Public"));
        }
        return result;
    }

    public Map<Long, Long> getProductCountByCategory(List<Categories> categories) {
        Map<Long, Long> result = new HashMap<>();
        for (Categories category : categories) {
            Long categoryId = category.getCategoryId();
            if (categoryId != null) {
                result.put(categoryId, countPublicProductsInCategory(categoryId));
            }
        }
        return result;
    }

    public Page<Products> getProductsPage(Long categoryId, int page, int size, String search) {
        PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1),
                Sort.by(Sort.Order.desc("totalSales"), Sort.Order.desc("createdAt")));
        if (search != null && !search.trim().isEmpty()) {
            return productsRepository.findByCategoryIdAndStatusAndSearch(categoryId, "Public", search.trim(), pageable);
        }
        return productsRepository.findByCategoryIdAndStatus(categoryId, "Public", pageable);
    }

    public List<Products> getProducts(Long categoryId, String search) {
        if (search != null && !search.trim().isEmpty()) {
            // Lấy tất cả products có search, sau đó sort
            var page = productsRepository.findByCategoryIdAndStatusAndSearch(
                    categoryId, "Public", search.trim(), 
                    PageRequest.of(0, Integer.MAX_VALUE, Sort.by(Sort.Order.desc("totalSales"), Sort.Order.desc("createdAt"))));
            return page.getContent();
        }
        // Lấy tất cả products, sau đó sort
        var page = productsRepository.findByCategoryIdAndStatus(
                categoryId, "Public", 
                PageRequest.of(0, Integer.MAX_VALUE, Sort.by(Sort.Order.desc("totalSales"), Sort.Order.desc("createdAt"))));
        return page.getContent();
    }

    public Map<Long, String> buildPrimaryImageMap(List<Products> products) {
        Map<Long, String> primaryImageByProduct = new HashMap<>();
        for (Products p : products) {
            String url = null;
            try {
                var images = productImagesRepository.findTop1ByProductIdAndIsPrimaryTrueOrderByImageIdAsc(p.getProductId());
                if (images != null && !images.isEmpty()) {
                    url = images.get(0).getImageUrl();
                }
            } catch (Exception ignored) {}
            primaryImageByProduct.put(p.getProductId(), url);
        }
        return primaryImageByProduct;
    }

    public Long getCartCount(Long userId) {
        if (userId == null) return 0L;
        // Hiển thị số lượng dòng trong giỏ ở header theo logic cũ
        return shoppingCartRepository.countByUserId(userId);
    }

    public List<ProductImages> getProductImagesForProducts(List<Products> products) {
        if (products == null || products.isEmpty()) {
            return List.of();
        }
        List<Long> productIds = products.stream()
                .map(Products::getProductId)
                .filter(id -> id != null)
                .toList();
        if (productIds.isEmpty()) {
            return List.of();
        }
        return productImagesRepository.findAll().stream()
                .filter(img -> productIds.contains(img.getProductId()))
                .toList();
    }
}


