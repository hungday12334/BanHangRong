package banhangrong.su25.service;

import banhangrong.su25.Entity.Products;
import banhangrong.su25.Entity.ProductReviews;
import banhangrong.su25.Entity.Users;
import banhangrong.su25.Repository.ProductsRepository;
import banhangrong.su25.Repository.ProductImagesRepository;
import banhangrong.su25.Repository.ProductReviewsRepository;
import banhangrong.su25.Repository.UsersRepository;
import banhangrong.su25.Repository.ShoppingCartRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductDetailService {

    private final ProductsRepository productsRepository;
    private final ProductImagesRepository productImagesRepository;
    private final ProductReviewsRepository productReviewsRepository;
    private final UsersRepository usersRepository;
    private final ShoppingCartRepository shoppingCartRepository;

    public ProductDetailService(ProductsRepository productsRepository,
                                ProductImagesRepository productImagesRepository,
                                ProductReviewsRepository productReviewsRepository,
                                UsersRepository usersRepository,
                                ShoppingCartRepository shoppingCartRepository) {
        this.productsRepository = productsRepository;
        this.productImagesRepository = productImagesRepository;
        this.productReviewsRepository = productReviewsRepository;
        this.usersRepository = usersRepository;
        this.shoppingCartRepository = shoppingCartRepository;
    }

    public Users getCurrentUserOrNull() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getName() == null) return null;
        return usersRepository.findByUsername(auth.getName()).orElse(null);
    }

    public Products getProductById(Long id) {
        return productsRepository.findById(id).orElse(null);
    }

    public List<?> getPrimaryImages(Long productId) {
        return productImagesRepository.findTop1ByProductIdAndIsPrimaryTrueOrderByImageIdAsc(productId);
    }

    public List<ProductReviews> getReviewsByProduct(Long productId) {
        return productReviewsRepository.findByProductIdOrderByCreatedAtDesc(productId);
    }

    public Long getCartCount(Long userId) {
        if (userId == null) return 0L;
        return shoppingCartRepository.countByUserId(userId);
    }
}


