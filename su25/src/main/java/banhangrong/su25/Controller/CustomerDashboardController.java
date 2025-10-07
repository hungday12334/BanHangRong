package banhangrong.su25.Controller;

import banhangrong.su25.Entity.Products;
import banhangrong.su25.Repository.ProductsRepository;
import banhangrong.su25.Repository.ProductImagesRepository;
import banhangrong.su25.Repository.UsersRepository;
import banhangrong.su25.Entity.Users;
import banhangrong.su25.Entity.ProductImages;
import banhangrong.su25.Repository.ShoppingCartRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.List;

@Controller
public class CustomerDashboardController {

    private final ProductsRepository productsRepository;
    private final ProductImagesRepository productImagesRepository;
    private final ShoppingCartRepository shoppingCartRepository;
    private final UsersRepository usersRepository;

    public CustomerDashboardController(ProductsRepository productsRepository, ProductImagesRepository productImagesRepository, ShoppingCartRepository shoppingCartRepository, UsersRepository usersRepository) {
        this.productsRepository = productsRepository;
        this.productImagesRepository = productImagesRepository;
        this.shoppingCartRepository = shoppingCartRepository;
        this.usersRepository = usersRepository;
    }

    @GetMapping("/customer/dashboard")
    public String customerDashboard(@RequestParam(name = "page", required = false, defaultValue = "0") int page,
                                    @RequestParam(name = "size", required = false, defaultValue = "15") int size,
                                    Model model) {
        // ORM: paginated active products ordered by total sales desc then created_at desc
        PageRequest pageable = PageRequest.of(Math.max(page,0), Math.max(size,1),
                Sort.by(Sort.Order.desc("totalSales"), Sort.Order.desc("createdAt")));
        Page<Products> featuredPage = productsRepository.findByIsActiveTrue(pageable);
        List<Products> featured = featuredPage.getContent();
        // Derive primary image directly from entity relations
        java.util.Map<Long, String> primaryImageByProduct = new java.util.HashMap<>();
        for (Products p : featured) {
            String url = null;
            var imgs = p.getImages();
            if (imgs != null && !imgs.isEmpty()) {
                for (var im : imgs) { if (Boolean.TRUE.equals(im.getIsPrimary())) { url = im.getImageUrl(); break; } }
                if (url == null) url = imgs.get(0).getImageUrl();
            }
            // Fallback: direct repository lookup in case relation isn't populated
            if (url == null || url.isBlank()) {
                var primary = productImagesRepository.findTop1ByProductIdAndIsPrimaryTrueOrderByImageIdAsc(p.getProductId());
                if (primary != null && !primary.isEmpty()) url = primary.get(0).getImageUrl();
                if (url == null || url.isBlank()) {
                    var any = productImagesRepository.findTop1ByProductIdOrderByImageIdAsc(p.getProductId());
                    if (any != null && !any.isEmpty()) url = any.get(0).getImageUrl();
                }
            }
            if (url != null && !url.isBlank()) primaryImageByProduct.put(p.getProductId(), url);
        }
        model.addAttribute("featuredProducts", featured);
        model.addAttribute("page", featuredPage.getNumber());
        model.addAttribute("totalPages", featuredPage.getTotalPages());
        model.addAttribute("size", featuredPage.getSize());
        model.addAttribute("primaryImageByProduct", primaryImageByProduct);
        // demo userId=2, show cart count in topbar
        try { model.addAttribute("cartCount", shoppingCartRepository.countByUserId(2L)); } catch (Exception ignored) {}
        try {
            Users currentUser = usersRepository.findAll().stream()
                    .sorted((a,b)-> Long.compare(a.getUserId(), b.getUserId()))
                    .findFirst().orElse(null);
            model.addAttribute("user", currentUser);
        } catch (Exception ignored) {}
        return "pages/customer_dashboard";
    }
}


