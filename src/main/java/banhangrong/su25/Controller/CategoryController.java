package banhangrong.su25.Controller;

import banhangrong.su25.Entity.Categories;
import banhangrong.su25.Entity.Products;
import banhangrong.su25.Entity.Users;
import banhangrong.su25.Repository.CategoriesRepository;
import banhangrong.su25.Repository.ProductsRepository;
import banhangrong.su25.Repository.ProductImagesRepository;
import banhangrong.su25.Repository.ShoppingCartRepository;
import banhangrong.su25.Repository.UsersRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

@Controller
public class CategoryController {

    private final CategoriesRepository categoriesRepository;
    private final ProductsRepository productsRepository;
    private final ProductImagesRepository productImagesRepository;
    private final ShoppingCartRepository shoppingCartRepository;
    private final UsersRepository usersRepository;

    public CategoryController(CategoriesRepository categoriesRepository, 
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

    @GetMapping("/categories")
    public String categoriesPage(Model model) {
        // Kiểm tra email verified cho CUSTOMER
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
            String username = auth.getName();
            Users currentUser = usersRepository.findByUsername(username).orElse(null);
            if (currentUser != null && "CUSTOMER".equals(currentUser.getUserType())) {
                if (!Boolean.TRUE.equals(currentUser.getIsEmailVerified())) {
                    return "redirect:/verify-email-required";
                }
            }
        }

        // Lấy danh sách tất cả categories
        List<Categories> categories = categoriesRepository.findCategoriesWithPublicProducts();
        
        // Đếm số products cho mỗi category
        Map<Long, Long> productCountByCategory = new HashMap<>();
        for (Categories category : categories) {
            Long count = productsRepository.countByCategoryIdAndStatus(category.getCategoryId(), "Public");
            productCountByCategory.put(category.getCategoryId(), count);
        }

        // Lấy cart count nếu user đã đăng nhập
        Long cartCount = 0L;
        try {
            if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
                String username = auth.getName();
                Users user = usersRepository.findByUsername(username).orElse(null);
                if (user != null) {
                    cartCount = shoppingCartRepository.countByUserId(user.getUserId());
                }
            }
        } catch (Exception ignored) {}

        model.addAttribute("categories", categories);
        model.addAttribute("productCountByCategory", productCountByCategory);
        model.addAttribute("cartCount", cartCount);
        model.addAttribute("user", (auth != null && !(auth instanceof AnonymousAuthenticationToken)) ? usersRepository.findByUsername(auth.getName()).orElse(null) : null);

        return "customer/categories";
    }

    @GetMapping("/category/{categoryId}")
    public String categoryProducts(@PathVariable Long categoryId,
                                @RequestParam(name = "page", required = false, defaultValue = "0") int page,
                                @RequestParam(name = "size", required = false, defaultValue = "15") int size,
                                @RequestParam(name = "search", required = false) String search,
                                Model model) {
        // Kiểm tra email verified cho CUSTOMER
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
            String username = auth.getName();
            Users currentUser = usersRepository.findByUsername(username).orElse(null);
            if (currentUser != null && "CUSTOMER".equals(currentUser.getUserType())) {
                if (!Boolean.TRUE.equals(currentUser.getIsEmailVerified())) {
                    return "redirect:/verify-email-required";
                }
            }
        }

        // Kiểm tra category có tồn tại không
        Categories category = categoriesRepository.findById(categoryId).orElse(null);
        if (category == null) {
            return "redirect:/categories";
        }

        // Tạo pageable với sắp xếp theo totalSales desc, createdAt desc
        PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1),
                Sort.by(Sort.Order.desc("totalSales"), Sort.Order.desc("createdAt")));

        Page<Products> productsPage;
        if (search != null && !search.trim().isEmpty()) {
            // Search mode
            productsPage = productsRepository.findByCategoryIdAndStatusAndSearch(
                categoryId, "Public", search.trim(), pageable);
        } else {
            // Normal mode
            productsPage = productsRepository.findByCategoryIdAndStatus(categoryId, "Public", pageable);
        }

        List<Products> products = productsPage.getContent();
        
        // Lấy primary image cho mỗi product
        Map<Long, String> primaryImageByProduct = new HashMap<>();
        for (Products p : products) {
            String url = null;
            try {
                var images = productImagesRepository.findTop1ByProductIdAndIsPrimaryTrueOrderByImageIdAsc(p.getProductId());
                if (!images.isEmpty()) {
                    url = images.get(0).getImageUrl();
                }
            } catch (Exception ignored) {}
            primaryImageByProduct.put(p.getProductId(), url);
        }

        // Lấy cart count nếu user đã đăng nhập
        Long cartCount = 0L;
        try {
            if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
                String username = auth.getName();
                Users user = usersRepository.findByUsername(username).orElse(null);
                if (user != null) {
                    cartCount = shoppingCartRepository.countByUserId(user.getUserId());
                }
            }
        } catch (Exception ignored) {}

        model.addAttribute("category", category);
        model.addAttribute("products", products);
        model.addAttribute("primaryImageByProduct", primaryImageByProduct);
        model.addAttribute("page", page);
        model.addAttribute("size", size);
        model.addAttribute("search", search);
        model.addAttribute("totalPages", productsPage.getTotalPages());
        model.addAttribute("totalElements", productsPage.getTotalElements());
        model.addAttribute("cartCount", cartCount);
        model.addAttribute("user", (auth != null && !(auth instanceof AnonymousAuthenticationToken)) ? usersRepository.findByUsername(auth.getName()).orElse(null) : null);

        return "customer/category-products";
    }
}
