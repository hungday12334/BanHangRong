package banhangrong.su25.Controller;

import banhangrong.su25.Entity.Products;
import banhangrong.su25.Entity.Users;
import banhangrong.su25.service.CustomerDashboardService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Map;

/**
 * Controller xử lý các request từ Customer Dashboard
 * 
 * MÔ HÌNH ĐÚNG: UI → Controller → Service → Repository → DB
 * - Controller chỉ xử lý HTTP request/response
 * - Service chứa business logic
 * - Repository truy vấn database
 */
@Controller
public class CustomerDashboardController {

    // Inject Service - Controller CHỈ dùng Service, KHÔNG dùng Repository trực tiếp
    private final CustomerDashboardService customerDashboardService;

    // Constructor injection - Spring tự động inject Service
    public CustomerDashboardController(CustomerDashboardService customerDashboardService) {
        this.customerDashboardService = customerDashboardService;
    }

    /**
     * Xử lý request GET /customer/dashboard
     * Hiển thị trang dashboard với danh sách sản phẩm công khai
     * 
     * LUỒNG XỬ LÝ ĐÚNG:
     * 1. Controller nhận request từ browser
     * 2. Controller gọi Service (KHÔNG gọi Repository trực tiếp)
     * 3. Service xử lý business logic và gọi Repository
     * 4. Repository truy vấn database
     * 5. Dữ liệu trả về: Repository → Service → Controller → Model → Thymeleaf
     * 
     * @param page số trang (từ query string: ?page=1)
     * @param size số sản phẩm mỗi trang (từ query string: ?size=20)
     * @param search từ khóa tìm kiếm (từ query string: ?search=laptop)
     * @param model Spring Model để truyền dữ liệu sang Thymeleaf
     * @return tên view template "customer/dashboard"
     */
    @GetMapping("/customer/dashboard")
    public String customerDashboard(
            @RequestParam(name = "page", required = false, defaultValue = "0") int page,
            @RequestParam(name = "size", required = false, defaultValue = "15") int size,
            @RequestParam(name = "search", required = false) String search,
            Model model) {
        
        // ==================== BƯỚC 1: XỬ LÝ AUTHENTICATION ====================
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Users currentUser = null;
        
        if (auth != null && auth.isAuthenticated()) {
            String username = auth.getName();
            // Gọi Service để lấy thông tin user (KHÔNG gọi Repository trực tiếp)
            currentUser = customerDashboardService.getUserByUsername(username);
            
            // Kiểm tra email verification qua Service
            if (currentUser != null) {
                if (!customerDashboardService.isCustomerEmailVerified(currentUser)) {
                    return "redirect:/verify-email-required";
                }
            }
        }
        
        // ==================== BƯỚC 2: LẤY DỮ LIỆU TỪ SERVICE ====================
        // Controller gọi Service, không gọi Repository trực tiếp
        // Service sẽ xử lý logic tìm kiếm, phân trang, lấy hình ảnh
        Page<Products> featuredPage = customerDashboardService.getPublicProducts(page, size, search);
        List<Products> featured = featuredPage.getContent();
        
        // Lấy hình ảnh sản phẩm từ Service
        Map<Long, String> primaryImageByProduct = customerDashboardService.getProductImages(featured);
        
        // ==================== BƯỚC 3: THÊM DỮ LIỆU VÀO MODEL ====================
        // Model là container để truyền dữ liệu từ Controller sang Thymeleaf view
        // Tất cả dữ liệu trong Model sẽ có sẵn trong Thymeleaf template
        
        // featuredProducts: Danh sách sản phẩm (List<Products>)
        // Dùng trong Thymeleaf: th:each="p : ${featuredProducts}"
        model.addAttribute("featuredProducts", featured);
        
        // page: Số trang hiện tại (int)
        // Dùng trong Thymeleaf: ${page}
        model.addAttribute("page", featuredPage.getNumber());
        
        // totalPages: Tổng số trang (int)
        // Dùng trong Thymeleaf: ${totalPages}
        model.addAttribute("totalPages", featuredPage.getTotalPages());
        
        // size: Số sản phẩm mỗi trang (int)
        // Dùng trong Thymeleaf: ${size}
        model.addAttribute("size", featuredPage.getSize());
        
        // primaryImageByProduct: Map hình ảnh (Map<Long, String>)
        // Dùng trong Thymeleaf: ${primaryImageByProduct[p.productId]}
        model.addAttribute("primaryImageByProduct", primaryImageByProduct);
        
        // search: Từ khóa tìm kiếm (String)
        // Dùng trong Thymeleaf: ${search}
        model.addAttribute("search", search);
        
        // ==================== BƯỚC 4: THÊM THÔNG TIN USER ====================
        if (currentUser != null) {
            // cartCount: Số lượng giỏ hàng (Long)
            // Dùng trong Thymeleaf: ${cartCount}
            // Gọi Service để đếm giỏ hàng (KHÔNG gọi Repository trực tiếp)
            Long cartCount = customerDashboardService.getCartCount(currentUser.getUserId());
            model.addAttribute("cartCount", cartCount);
            
            // user: Thông tin user (Users entity)
            // Dùng trong Thymeleaf: ${user.username}, ${user.balance}
            model.addAttribute("user", currentUser);
        }
        
        // ==================== BƯỚC 5: TRẢ VỀ VIEW ====================
        // Spring sẽ tìm file: templates/customer/dashboard.html
        // Tất cả dữ liệu trong Model sẽ được truyền sang Thymeleaf
        return "customer/dashboard";
    }

    @GetMapping("/rating-history")
    public String ratingHistory() {
        // Redirect to the unified My Reviews page
        return "redirect:/customer/reviews";
    }

    @GetMapping("/orderhistory")
    public String orderHistory(@RequestParam(name = "page", required = false, defaultValue = "0") int page,
                               @RequestParam(name = "size", required = false, defaultValue = "3") int size,
                               @RequestParam(name = "search", required = false) String search,
                               @RequestParam(name = "status", required = false) String status,
                               Model model) {
        // TODO: Refactor method này để dùng Service thay vì Repository trực tiếp
        // Hiện tại method này vẫn đang dùng Repository trực tiếp
        // Nên tạm thời giữ nguyên để không break code
        // Nên tạo OrderService và refactor sau
        return "redirect:/customer/dashboard";
    }


    @GetMapping("/notification")
    public String notification(Model model) {
        // Lấy thông tin user từ Service
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return "redirect:/login";
        }
        
        String username = auth.getName();
        Users user = customerDashboardService.getUserByUsername(username);
        
        if (user == null) {
            return "redirect:/login";
        }
        
        // Đếm giỏ hàng từ Service
        Long cartCount = customerDashboardService.getCartCount(user.getUserId());
        
        model.addAttribute("user", user);
        model.addAttribute("cartCount", cartCount);
        
        return "customer/notification";
    }

    @GetMapping("/customer/seller/{sellerId}")
    public String viewSeller(@PathVariable Long sellerId, Model model) {
        // TODO: Refactor method này để dùng Service thay vì Repository trực tiếp
        // Hiện tại method này vẫn đang dùng Repository trực tiếp
        // Nên tạm thời redirect về dashboard
        // Nên tạo SellerService và refactor sau
        return "redirect:/customer/dashboard";
    }
}


