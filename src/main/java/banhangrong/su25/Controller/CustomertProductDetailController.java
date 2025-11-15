package banhangrong.su25.Controller;

import banhangrong.su25.Entity.ProductImages;
import banhangrong.su25.Entity.ProductReviews;
import banhangrong.su25.Entity.Products;
import banhangrong.su25.Entity.Users;
import banhangrong.su25.Repository.UsersRepository;
import banhangrong.su25.service.ProductImageService;
import banhangrong.su25.service.ProductReviewService;
import banhangrong.su25.service.ProductService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.util.Optional;

@Controller
public class CustomertProductDetailController {
    private ProductService proService;
    private ProductImageService productImageService;
    private ProductReviewService productReviewService;
    private UsersRepository usersRepository;

    public CustomertProductDetailController(ProductService proService, ProductImageService productImageService, ProductReviewService productReviewService, UsersRepository usersRepository) {
        this.proService = proService;
        this.productImageService = productImageService;
        this.productReviewService = productReviewService;
        this.usersRepository = usersRepository;
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
        
        // Get seller info for chat
        if (product != null && product.getSellerId() != null) {
            Optional<Users> sellerOpt = usersRepository.findById(product.getSellerId());
            if (sellerOpt.isPresent()) {
                Users seller = sellerOpt.get();
                model.addAttribute("seller", seller);
                model.addAttribute("sellerId", seller.getUserId());
            }
        }

        List<ProductReviews> reviews = productReviewService.getReviewsByProductId(id);
        model.addAttribute("reviews", reviews);
        
        return "customer/product_detail";
    }
}
