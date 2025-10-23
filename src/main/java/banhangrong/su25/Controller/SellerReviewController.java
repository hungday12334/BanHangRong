package banhangrong.su25.Controller;

import banhangrong.su25.Entity.ProductReviews;
import banhangrong.su25.service.ProductReviewService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/seller/reviews")
public class SellerReviewController {

    private final ProductReviewService productReviewService;

    public SellerReviewController(ProductReviewService productReviewService) {
        this.productReviewService = productReviewService;
    }

    @GetMapping
    public String reviewsDashboard(Model model) {
        // For demo, using seller ID 1
        Long sellerId = 1L;

        List<ProductReviews> allReviews = productReviewService.getSellerReviews(sellerId);
        List<ProductReviews> unansweredReviews = productReviewService.getUnansweredReviews(sellerId);
        Long unansweredCount = productReviewService.getUnansweredReviewCount(sellerId);

        model.addAttribute("allReviews", allReviews);
        model.addAttribute("unansweredReviews", unansweredReviews);
        model.addAttribute("unansweredCount", unansweredCount);
        model.addAttribute("sellerId", sellerId);

        return "seller/reviews";
    }

    @PostMapping("/respond/{reviewId}")
    @ResponseBody
    public Map<String, Object> respondToReview(@PathVariable Long reviewId,
                                               @RequestBody Map<String, String> request) {
        try {
            String response = request.get("response");
            ProductReviews updatedReview = productReviewService.addSellerResponse(reviewId, response);

            return Map.of(
                    "success", true,
                    "message", "Đã gửi phản hồi thành công",
                    "review", updatedReview
            );
        } catch (Exception e) {
            return Map.of(
                    "success", false,
                    "message", "Lỗi: " + e.getMessage()
            );
        }
    }

    @GetMapping("/api/unanswered-count")
    @ResponseBody
    public Map<String, Object> getUnansweredCount(@RequestParam Long sellerId) {
        Long count = productReviewService.getUnansweredReviewCount(sellerId);
        return Map.of("unansweredCount", count);
    }
}
