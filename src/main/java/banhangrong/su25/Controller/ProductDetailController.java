package banhangrong.su25.Controller;

import banhangrong.su25.Entity.Products;
import banhangrong.su25.Repository.ProductsRepository;
import banhangrong.su25.Repository.ProductImagesRepository;
import banhangrong.su25.Repository.ProductReviewsRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Collections;
import java.util.List;

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
    public String productDetail(@PathVariable("id") String idStr, Model model) {
        try {
            if (idStr == null || idStr.trim().isEmpty()) {
                return "redirect:/customer/dashboard";
            }

            Long id;
            try {
                id = Long.parseLong(idStr);
            } catch (NumberFormatException e) {
                return "redirect:/customer/dashboard";
            }

            if (id == null || id <= 0) {
                return "redirect:/customer/dashboard";
            }

            Products p = productsRepository.findById(id).orElse(null);
            if (p == null) {
                return "redirect:/customer/dashboard";
            }

            model.addAttribute("product", p);

            try {
                List<?> images = productImagesRepository.findTop1ByProductIdAndIsPrimaryTrueOrderByImageIdAsc(id);
                model.addAttribute("images", images != null ? images : Collections.emptyList());
            } catch (Exception e) {
                model.addAttribute("images", Collections.emptyList());
            }

            try {
                List<?> reviews = productReviewsRepository.findByProductIdOrderByCreatedAtDesc(id);
                model.addAttribute("reviews", reviews != null ? reviews : Collections.emptyList());
            } catch (Exception e) {
                model.addAttribute("reviews", Collections.emptyList());
            }

            return "customer/product_detail";
        } catch (Exception e) {
            e.printStackTrace();
            return "redirect:/customer/dashboard";
        }
    }
}


