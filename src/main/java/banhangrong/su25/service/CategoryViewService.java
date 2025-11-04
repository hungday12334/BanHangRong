package banhangrong.su25.service;

import banhangrong.su25.Entity.Categories;
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
        return categoriesRepository.findById(categoryId).orElse(null);
    }

    public long countPublicProductsInCategory(Long categoryId) {
        return productsRepository.countByCategoryIdAndStatus(categoryId, "Public");
    }

    public Page<Products> getProductsPage(Long categoryId, int page, int size, String search) {
        PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1),
                Sort.by(Sort.Order.desc("totalSales"), Sort.Order.desc("createdAt")));
        if (search != null && !search.trim().isEmpty()) {
            return productsRepository.findByCategoryIdAndStatusAndSearch(categoryId, "Public", search.trim(), pageable);
        }
        return productsRepository.findByCategoryIdAndStatus(categoryId, "Public", pageable);
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
}


