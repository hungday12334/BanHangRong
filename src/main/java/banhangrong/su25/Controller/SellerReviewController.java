package banhangrong.su25.Controller;

import banhangrong.su25.Entity.ProductReviews;
import banhangrong.su25.Entity.Users;
import banhangrong.su25.Repository.UsersRepository;
import banhangrong.su25.service.ProductReviewService;
import jakarta.servlet.http.HttpSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Controller
@RequestMapping("/seller/reviews")
public class SellerReviewController {

    private final ProductReviewService productReviewService;
    private final UsersRepository usersRepository;
    private static final int PAGE_SIZE = 5; // 5 reviews per page

    public SellerReviewController(ProductReviewService productReviewService, UsersRepository usersRepository) {
        this.productReviewService = productReviewService;
        this.usersRepository = usersRepository;
    }

    @GetMapping
    public String reviewsDashboard(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer ratingFrom,
            @RequestParam(required = false) Integer ratingTo,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate,
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) String customerName,
            Model model,
            HttpSession session) {

        // SEC-01: Get authenticated user from Spring Security
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Users currentUser = null;
        Long sellerId = null;
        String userRole = null;

        if (auth != null && auth.isAuthenticated() && !auth.getName().equals("anonymousUser")) {
            String username = auth.getName();
            currentUser = usersRepository.findByUsername(username).orElse(null);

            if (currentUser != null) {
                sellerId = currentUser.getUserId();
                userRole = currentUser.getUserType();
            }
        }

        // SEC-02: Enforce authentication - redirect to login if not authenticated
        if (sellerId == null || userRole == null) {
            return "redirect:/login?error=notAuthenticated";
        }

        // SEC-03: Enforce authorization - only SELLER role can access
        if (!"SELLER".equals(userRole)) {
            return "redirect:/login?error=unauthorized";
        }

        // Pagination
        Pageable pageable = PageRequest.of(page, PAGE_SIZE, Sort.by("createdAt").descending());

        // Filter reviews based on parameters (with new rating range and customer name)
        Page<ProductReviews> reviewsPage = productReviewService.getFilteredReviews(
            sellerId, status, ratingFrom, ratingTo, fromDate, toDate, productId, customerName, pageable
        );

        // Get counts for KPI cards
        Long totalCount = productReviewService.getTotalReviewCount(sellerId);
        Long unansweredCount = productReviewService.getUnansweredReviewCount(sellerId);
        Long answeredCount = totalCount - unansweredCount;

        model.addAttribute("reviews", reviewsPage.getContent());
        model.addAttribute("reviewsPage", reviewsPage);
        model.addAttribute("totalCount", totalCount);
        model.addAttribute("unansweredCount", unansweredCount);
        model.addAttribute("answeredCount", answeredCount);
        model.addAttribute("sellerId", sellerId);
        model.addAttribute("currentPage", page);

        // Pass filter params back to view (updated with new parameters)
        model.addAttribute("filterStatus", status);
        model.addAttribute("filterRatingFrom", ratingFrom);
        model.addAttribute("filterRatingTo", ratingTo);
        model.addAttribute("filterFromDate", fromDate);
        model.addAttribute("filterToDate", toDate);
        model.addAttribute("filterProductId", productId);
        model.addAttribute("filterCustomerName", customerName);

        return "seller/reviews";
    }

    @PostMapping("/respond/{reviewId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> respondToReview(
            @PathVariable Long reviewId,
            @RequestBody Map<String, String> request,
            HttpSession session) {

        // SEC-01: Get authenticated user from Spring Security
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Users currentUser = null;
        Long sellerId = null;
        String userRole = null;

        if (auth != null && auth.isAuthenticated() && !auth.getName().equals("anonymousUser")) {
            String username = auth.getName();
            currentUser = usersRepository.findByUsername(username).orElse(null);

            if (currentUser != null) {
                sellerId = currentUser.getUserId();
                userRole = currentUser.getUserType();
            }
        }

        if (sellerId == null || userRole == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", "Please login to respond to reviews"));
        }

        // SEC-02: Enforce authorization - only SELLER role
        if (!"SELLER".equals(userRole)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", "Please login with a seller account"));
        }

        // FIX VAL-02: Validate review ID
        if (reviewId == null || reviewId <= 0) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "Invalid review ID"));
        }

        // FIX UC-02, VAL-01: Validate response input
        String response = request.get("response");
        if (response == null || response.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "Response cannot be empty"));
        }

        // FIX EDGE-03: Validate max length
        if (response.length() > 1000) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "Response cannot exceed 1000 characters"));
        }

        // FIX EDGE-07: Sanitize HTML to prevent XSS
        response = response.trim()
                .replaceAll("<", "&lt;")
                .replaceAll(">", "&gt;");

        try {
            // FIX SEC-03: Validate ownership - review có thuộc seller này không?
            if (!productReviewService.isReviewOwnedBySeller(reviewId, sellerId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("success", false, "message", "You do not have permission to respond to this review"));
            }

            ProductReviews updatedReview = productReviewService.addSellerResponse(reviewId, response);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Response sent successfully",
                    "review", updatedReview
            ));

        } catch (IllegalArgumentException e) {
            // FIX UC-01: Handle review not found
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            // FIX: Better error handling
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "An error occurred: " + e.getMessage()));
        }
    }

    @GetMapping("/api/unanswered-count")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getUnansweredCount(
            @RequestParam Long sellerId,
            HttpSession session) {

        // SEC-01: Get authenticated user from Spring Security
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Users currentUser = null;
        Long currentSellerId = null;
        String userRole = null;

        if (auth != null && auth.isAuthenticated() && !auth.getName().equals("anonymousUser")) {
            String username = auth.getName();
            currentUser = usersRepository.findByUsername(username).orElse(null);

            if (currentUser != null) {
                currentSellerId = currentUser.getUserId();
                userRole = currentUser.getUserType();
            }
        }

        if (currentSellerId == null || userRole == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Not authenticated"));
        }

        // SEC-02: Enforce authorization - only SELLER role
        if (!"SELLER".equals(userRole)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Unauthorized"));
        }

        // FIX VAL-03: Validate seller ID
        if (sellerId == null || sellerId <= 0) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Invalid seller ID"));
        }

        // FIX SEC-03: Không cho phép xem count của seller khác
        if (!currentSellerId.equals(sellerId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "You can only view your own statistics"));
        }

        Long count = productReviewService.getUnansweredReviewCount(sellerId);
        return ResponseEntity.ok(Map.of("unansweredCount", count));
    }
}

