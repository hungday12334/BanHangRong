package banhangrong.su25.Controller;

import banhangrong.su25.Entity.ProductImages;
import banhangrong.su25.Entity.ProductReviews;
import banhangrong.su25.Entity.Products;
import banhangrong.su25.service.ProductImageService;
import banhangrong.su25.service.ProductReviewService;
import banhangrong.su25.service.ProductService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@Controller
public class CustomertProductDetailController {
    private ProductService proService;
    private ProductImageService productImageService;
    private ProductReviewService productReviewService;

    public CustomertProductDetailController(ProductService proService, ProductImageService productImageService, ProductReviewService productReviewService) {
        this.proService = proService;
        this.productImageService = productImageService;
        this.productReviewService = productReviewService;
    }

    @GetMapping("product/{id}")
    public String productDetail(Model model,@PathVariable("id") Long id){
        Products product = proService.getProductById(id);
        

        ProductImages productImages = productImageService.getProductImagesById(id);
        if (productImages != null) {
            model.addAttribute("imgUrl", productImages.getImageUrl());
        } else {
            model.addAttribute("imgUrl", null);
        }
        
        model.addAttribute("product", product);
        model.addAttribute("totalReview", productReviewService.countByPid(id));
        model.addAttribute("avgRating", productReviewService.getAvgRatingByPid(id));
        

        List<ProductReviews> reviews = productReviewService.getReviewsByProductId(id);
        model.addAttribute("reviews", reviews);
        
        return "customer/product_detail";
    }
}
