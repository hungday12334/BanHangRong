package banhangrong.su25.Controller;

import banhangrong.su25.Entity.Products;
import banhangrong.su25.Repository.ProductsRepository;
import banhangrong.su25.Repository.ProductImagesRepository;
import banhangrong.su25.Repository.ProductReviewsRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class ProductDetailController {

    private final ProductsRepository productsRepository;
    private final ProductImagesRepository productImagesRepository;
    private final ProductReviewsRepository productReviewsRepository;

    public ProductDetailController(ProductsRepository productsRepository,
                                   ProductImagesRepository productImagesRepository,
                                   ProductReviewsRepository productReviewsRepository) {
        this.productsRepository = productsRepository;
        this.productImagesRepository = productImagesRepository;
        this.productReviewsRepository = productReviewsRepository;
    }

    @GetMapping("/product/{id}")
    public String productDetail(@PathVariable("id") Long id, Model model) {
        Products p = productsRepository.findById(id).orElse(null);
        if (p == null) return "redirect:/customer/dashboard";
        model.addAttribute("product", p);
        model.addAttribute("images", productImagesRepository.findTop1ByProductIdAndIsPrimaryTrueOrderByImageIdAsc(id));
        model.addAttribute("reviews", productReviewsRepository.findByProductIdOrderByCreatedAtDesc(id));
        return "customer/product_detail";
    }
}


