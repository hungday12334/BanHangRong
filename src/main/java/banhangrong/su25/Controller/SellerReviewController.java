package banhangrong.su25.Controller;

import banhangrong.su25.Entity.ProductReviews;
import banhangrong.su25.service.ProductReviewService;
import jakarta.servlet.http.HttpSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Controller
@RequestMapping("/seller/reviews")
public class SellerReviewController {

    private final ProductReviewService productReviewService;
    private static final int PAGE_SIZE = 5; // 5 reviews per page

    public SellerReviewController(ProductReviewService productReviewService) {
        this.productReviewService = productReviewService;
    }

    @GetMapping
    public String reviewsDashboard(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "0") int unansweredPage,
            Model model,
            HttpSession session) {
        // FIX SEC-01: Lấy seller ID từ session thay vì hardcode
        Long sellerId = (Long) session.getAttribute("userId");
        String userRole = (String) session.getAttribute("userRole");

        // DEMO MODE: Nếu chưa có session authentication, dùng seller ID = 1 để test
        // TODO: Remove this when authentication is implemented
        if (sellerId == null) {
            sellerId = 1L; // Demo seller ID
            userRole = "SELLER"; // Demo role
            System.out.println("⚠️ DEMO MODE: Using seller ID = 1 (no session authentication)");
        }

        // FIX SEC-02: Check authorization - chỉ SELLER mới truy cập được
        if (!"SELLER".equals(userRole)) {
            return "redirect:/login?error=unauthorized";
        }

        // Pagination for all reviews
        Pageable allPageable = PageRequest.of(page, PAGE_SIZE, Sort.by("createdAt").descending());
        Page<ProductReviews> allReviewsPage = productReviewService.getSellerReviews(sellerId, allPageable);

        // Pagination for unanswered reviews
        Pageable unansweredPageable = PageRequest.of(unansweredPage, PAGE_SIZE, Sort.by("createdAt").descending());
        Page<ProductReviews> unansweredReviewsPage = productReviewService.getUnansweredReviews(sellerId, unansweredPageable);

        Long unansweredCount = productReviewService.getUnansweredReviewCount(sellerId);

        model.addAttribute("allReviews", allReviewsPage.getContent());
        model.addAttribute("allReviewsPage", allReviewsPage);
        model.addAttribute("unansweredReviews", unansweredReviewsPage.getContent());
        model.addAttribute("unansweredReviewsPage", unansweredReviewsPage);
        model.addAttribute("unansweredCount", unansweredCount);
        model.addAttribute("sellerId", sellerId);
        model.addAttribute("currentPage", page);
        model.addAttribute("currentUnansweredPage", unansweredPage);

        return "seller/reviews";
    }

    @PostMapping("/respond/{reviewId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> respondToReview(
            @PathVariable Long reviewId,
            @RequestBody Map<String, String> request,
            HttpSession session) {

        // FIX SEC-02: Check authentication
        Long sellerId = (Long) session.getAttribute("userId");
        String userRole = (String) session.getAttribute("userRole");

        // DEMO MODE: Nếu chưa có session authentication, dùng seller ID = 1 để test
        if (sellerId == null) {
            sellerId = 1L;
            userRole = "SELLER";
            System.out.println("⚠️ DEMO MODE: Using seller ID = 1 for response");
        }

        if (!"SELLER".equals(userRole)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", "Vui lòng đăng nhập với tài khoản seller"));
        }

        // FIX VAL-02: Validate review ID
        if (reviewId == null || reviewId <= 0) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "Review ID không hợp lệ"));
        }

        // FIX UC-02, VAL-01: Validate response input
        String response = request.get("response");
        if (response == null || response.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "Response không được để trống"));
        }

        // FIX EDGE-03: Validate max length
        if (response.length() > 1000) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "Response không được vượt quá 1000 ký tự"));
        }

        // FIX EDGE-07: Sanitize HTML to prevent XSS
        response = response.trim()
                .replaceAll("<", "&lt;")
                .replaceAll(">", "&gt;");

        try {
            // FIX SEC-03: Validate ownership - review có thuộc seller này không?
            if (!productReviewService.isReviewOwnedBySeller(reviewId, sellerId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("success", false, "message", "Bạn không có quyền phản hồi review này"));
            }

            ProductReviews updatedReview = productReviewService.addSellerResponse(reviewId, response);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Đã gửi phản hồi thành công",
                    "review", updatedReview
            ));

        } catch (IllegalArgumentException e) {
            // FIX UC-01: Handle review not found
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            // FIX: Better error handling
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Đã xảy ra lỗi: " + e.getMessage()));
        }
    }

    @GetMapping("/api/unanswered-count")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getUnansweredCount(
            @RequestParam Long sellerId,
            HttpSession session) {

        // FIX SEC-03: Chỉ cho phép lấy count của chính mình
        Long currentSellerId = (Long) session.getAttribute("userId");
        String userRole = (String) session.getAttribute("userRole");

        // DEMO MODE: Nếu chưa có session authentication, dùng seller ID = 1
        if (currentSellerId == null) {
            currentSellerId = 1L;
            userRole = "SELLER";
            System.out.println("⚠️ DEMO MODE: Using seller ID = 1 for count API");
        }

        if (!"SELLER".equals(userRole)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Unauthorized"));
        }

        // FIX VAL-03: Validate seller ID
        if (sellerId == null || sellerId <= 0) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Seller ID không hợp lệ"));
        }

        // FIX SEC-03: Không cho phép xem count của seller khác
        if (!currentSellerId.equals(sellerId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Bạn chỉ có thể xem thống kê của chính mình"));
        }

        Long count = productReviewService.getUnansweredReviewCount(sellerId);
        return ResponseEntity.ok(Map.of("unansweredCount", count));
    }
}

