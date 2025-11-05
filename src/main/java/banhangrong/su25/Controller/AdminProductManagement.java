package banhangrong.su25.Controller;

import banhangrong.su25.DTO.ProductFilter;
import banhangrong.su25.Entity.Products;
import banhangrong.su25.Entity.Users;
import banhangrong.su25.email.Email;
import banhangrong.su25.email.EmailService;
import banhangrong.su25.service.AdminProductService;
import banhangrong.su25.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/admin/product")
public class AdminProductManagement {
    @Autowired
    AdminProductService adminProductService;
    @Autowired
    private EmailService emailService;
    @Autowired
    private UserService userService;

    @GetMapping("/filter")
    public String filterProduct(@ModelAttribute("filter") ProductFilter productFilter, RedirectAttributes redirectAttributes) {
        List<Products> lisFilterProduct = adminProductService.filter(productFilter);
        redirectAttributes.addFlashAttribute("filter", lisFilterProduct);
        redirectAttributes.addFlashAttribute("isFromFilter", true);
        return "redirect:/admin/products";
    }

    @GetMapping("/update")
    public String showUpdateForm(HttpServletRequest request,
                                 Model model,
                                 RedirectAttributes redirectAttributes) {
        String sId = request.getParameter("id");
        if (sId == null || sId.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Product not found");
            return "redirect:/admin/products";
        }

        Long id = Long.parseLong(sId);
        Products product = adminProductService.findById(id);

        if (product == null) {
            redirectAttributes.addFlashAttribute("error", "Product not found");
            return "redirect:/admin/products";
        }

        model.addAttribute("product", product);
        return "admin/product-update";
    }

    @PostMapping("/update")
    public String updateProduct(HttpServletRequest request,
                                RedirectAttributes redirectAttributes,
                                Model model) {
        String sId = request.getParameter("productId");
        if (sId == null || sId.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Product not found");
            return "redirect:/admin/products";
        }
        Long id = Long.parseLong(sId);
        Products product = adminProductService.findById(id);
        if (product == null || product.getStatus() == null) {
            if (!product.getStatus().equalsIgnoreCase("pending")) {
                redirectAttributes.addFlashAttribute("error", "Only Pending product can be Approved");
                return "redirect:/admin/products";
            }
            redirectAttributes.addFlashAttribute("error", "Product not found");
            return "redirect:/admin/products";
        }
        Users seller = userService.findById(product.getSellerId());
        product.setStatus("public");
        product.setUpdatedAt(LocalDateTime.now());
        adminProductService.save(product);
        String subject = "BanHangRong - Sản phẩm của bạn đã được duyệt!";

        String message = """
                <div style="font-family: Arial, sans-serif; color: #333; max-width: 600px; margin: auto; border: 1px solid #eee; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 12px rgba(0,0,0,0.05);">
                    <!-- Header xanh lá -->
                    <div style="background: linear-gradient(135deg, #28a745, #20c997); padding: 20px; text-align: center; color: white;">
                        <h2 style="margin: 0; font-size: 24px;">
                            <i class="bi bi-check-circle-fill"></i> Sản phẩm đã được duyệt!
                        </h2>
                    </div>
                
                    <!-- Nội dung -->
                    <div style="padding: 25px; background-color: #fff;">
                        <p style="font-size: 16px; line-height: 1.6;">
                            Xin chào <strong>%s</strong>,
                        </p>
                        <p style="font-size: 16px; line-height: 1.6;">
                            Chúng tôi rất vui mừng thông báo rằng sản phẩm của bạn đã được <strong>duyệt thành công</strong> bởi quản trị viên!
                        </p>
                
                        <div style="background-color: #f8fff9; border-left: 4px solid #28a745; padding: 15px; margin: 20px 0; font-size: 15px;">
                            <p style="margin: 0;"><strong>Tên sản phẩm:</strong> %s</p>
                            <p style="margin: 8px 0 0;"><strong>ID sản phẩm:</strong> %d</p>
                        </div>
                
                        <p style="font-size: 16px; line-height: 1.6;">
                            Bây giờ sản phẩm của bạn đã <strong>hiển thị công khai</strong> và khách hàng có thể xem, mua hoặc tải về (nếu là sản phẩm số).
                        </p>
                
                        <div style="text-align: center; margin: 30px 0;">
                            <a href="%s" 
                               style="background-color: #28a745; color: white; padding: 12px 30px; text-decoration: none; border-radius: 8px; font-weight: bold; display: inline-block; box-shadow: 0 4px 8px rgba(40,167,69,0.3);">
                               Xem sản phẩm của bạn
                            </a>
                        </div>
                
                        <p style="font-size: 14px; color: #666; line-height: 1.6;">
                            Nếu bạn có bất kỳ câu hỏi nào, vui lòng liên hệ hỗ trợ qua email: 
                            <a href="mailto:bonhoangncd@gmail.com" style="color: #28a745;">bonhoangncd@gmail.com</a>
                        </p>
                    </div>
                
                    <!-- Footer -->
                    <div style="background-color: #f8f9fa; padding: 15px; text-align: center; font-size: 13px; color: #777; border-top: 1px solid #eee;">
                        <p style="margin: 5px 0;">
                            Trân trọng,<br>
                            <strong>Đội ngũ BanHangRong</strong>
                        </p>
                        <p style="margin: 8px 0 0; font-size: 12px;">
                            © 2025 BanHangRong. All rights reserved.
                        </p>
                    </div>
                </div>
                """.formatted(
                seller.getUsername(),
                product.getName(),                                                   // Tên sản phẩm
                product.getProductId(),                                              // ID sản phẩm
                "https://banhangrong.com/product/" + product.getProductId()          // Link xem sản phẩm (thay bằng domain thật)
        );

        try {
            emailService.sendEmail(new Email(seller.getEmail(), subject, message));
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Gửi email thông báo thất bại: " + e.getMessage());
        }
        redirectAttributes.addFlashAttribute("success", "Product updated successfully");
        return "redirect:/admin/products";
    }

    @PostMapping("/cancel")
    public String cancelProduct(HttpServletRequest request,
                                RedirectAttributes redirectAttributes,
                                Model model) {
        String sId = request.getParameter("productId");
        if (sId == null || sId.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Product not found");
            return "redirect:/admin/products";
        }
        Long id = Long.parseLong(sId);
        Products product = adminProductService.findById(id);
        if (product == null || product.getStatus() == null) {
            redirectAttributes.addFlashAttribute("error", "Product not found");
            return "redirect:/admin/products";
        }
        if (!product.getStatus().equalsIgnoreCase("pending") && !product.getStatus().equalsIgnoreCase("public")) {
            redirectAttributes.addFlashAttribute("error", "Only Public or Pending product can be cancelled");
            return "redirect:/admin/products";
        }
        Users seller = userService.findById(product.getSellerId());
        String reason = request.getParameter("reason");
        product.setStatus("Cancelled");
        product.setUpdatedAt(LocalDateTime.now());
        adminProductService.save(product);
        sendCancellationEmail(seller, product, reason);
        redirectAttributes.addFlashAttribute("success", "Cancelled successfully");
        return "redirect:/admin/products";
    }
    private void sendCancellationEmail(Users seller, Products product, String reason) {
        String subject = "BanHangRong - Sản phẩm của bạn đã bị hủy";

        String message = """
        <div style="font-family: Arial, sans-serif; color: #333; max-width: 600px; margin: auto; border: 1px solid #eee; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 12px rgba(0,0,0,0.05);">
            <!-- Header đỏ cảnh báo -->
            <div style="background: linear-gradient(135deg, #dc3545, #e74c3c); padding: 20px; text-align: center; color: white;">
                <h2 style="margin: 0; font-size: 24px;">
                    Sản phẩm đã bị hủy
                </h2>
            </div>

            <!-- Nội dung -->
            <div style="padding: 25px; background-color: #fff;">
                <p style="font-size: 16px; line-height: 1.6;">
                    Xin chào <strong>%s</strong>,
                </p>
                <p style="font-size: 16px; line-height: 1.6;">
                    Chúng tôi rất tiếc phải thông báo rằng sản phẩm của bạn đã bị <strong>hủy bởi quản trị viên</strong>.
                </p>

                <div style="background-color: #fff5f5; border-left: 4px solid #dc3545; padding: 15px; margin: 20px 0; font-size: 15px;">
                    <p style="margin: 0;"><strong>Tên sản phẩm:</strong> %s</p>
                    <p style="margin: 8px 0 0;"><strong>ID sản phẩm:</strong> %d</p>
                </div>

                <div style="background-color: #fdf2f2; border: 1px solid #f5c6cb; border-radius: 8px; padding: 15px; margin: 20px 0; font-size: 15px; color: #721c24;">
                    <p style="margin: 0; font-weight: bold;">Lý do hủy:</p>
                    <p style="margin: 8px 0 0; font-style: italic;">"%s"</p>
                </div>

                <p style="font-size: 16px; line-height: 1.6;">
                    Sản phẩm này <strong>không còn hiển thị</strong> trên hệ thống. Nếu bạn cần chỉnh sửa và đăng lại, vui lòng tạo sản phẩm mới.
                </p>

                <p style="font-size: 14px; color: #666; line-height: 1.6;">
                    Nếu bạn cho rằng đây là nhầm lẫn, vui lòng liên hệ hỗ trợ qua email: 
                    <a href="mailto:bonhoangncd@gmail.com" style="color: #dc3545; font-weight: bold;">bonhoangncd@gmail.com</a>
                </p>
            </div>

            <!-- Footer -->
            <div style="background-color: #f8f9fa; padding: 15px; text-align: center; font-size: 13px; color: #777; border-top: 1px solid #eee;">
                <p style="margin: 5px 0;">
                    Trân trọng,<br>
                    <strong>Đội ngũ BanHangRong</strong>
                </p>
                <p style="margin: 8px 0 0; font-size: 12px;">
                    © 2025 BanHangRong. All rights reserved.
                </p>
            </div>
        </div>
        """.formatted(
                seller.getUsername(),
                product.getName(),
                product.getProductId(),
                reason
        );

        try {
            emailService.sendEmail(new Email(seller.getEmail(), subject, message));
        } catch (Exception e) {
            // Ghi log, không làm hỏng flow chính
            System.err.println("Failed to send cancellation email to " + seller.getEmail() + ": " + e.getMessage());
        }
    }
}
