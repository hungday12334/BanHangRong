package banhangrong.su25.email;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Service
public class EmailService {

    @Autowired(required = false)
    private JavaMailSender mailSender;

    // Gửi email chung
    public void sendEmail(Email email) {
        System.out.println("=== EMAIL SERVICE: Starting to send email ===");
        System.out.println("To: " + email.getToEmail());
        System.out.println("Subject: " + email.getSubject());

        if (mailSender == null) {
            System.err.println("❌ ERROR: JavaMailSender is not configured. Email not sent to: " + email.getToEmail());
            System.err.println("Please check application.properties for email configuration");
            return;
        }
        
        MimeMessage message = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom("Ban Hang Rong <bonhoangncd@gmail.com>");
            helper.setTo(email.getToEmail());
            helper.setSubject(email.getSubject());
            helper.setText(email.getBody(), false);

            System.out.println("📧 Sending email...");
            mailSender.send(message);
            System.out.println("✅ Email sent successfully to: " + email.getToEmail());

        } catch (MessagingException e) {
            System.err.println("❌ ERROR sending email: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Error sending email: " + e.getMessage(), e);
        } catch (Exception e) {
            System.err.println("❌ UNEXPECTED ERROR: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Unexpected error while sending email: " + e.getMessage(), e);
        }
    }

    // Gửi email reset password (AuthController đang dùng)
    public void sendPasswordResetEmail(String to, String token) {
        String subject = "Password Reset Request";
        String resetLink = "http://localhost:8080/reset-password?token=" + token;
        String body = "Click the link to reset your password: " + resetLink;

        Email email = new Email(to, subject, body);
        sendEmail(email);
    }

    /**
     * Gửi email xác nhận đơn hàng thành công
     */
    public void sendOrderConfirmationEmail(String toEmail, String customerName, String orderCode, 
                                          Long orderId, BigDecimal totalAmount, LocalDateTime orderDate,
                                          List<OrderItemInfo> orderItems, List<LicenseKeyInfo> licenseKeys) {
        String subject = "Xác nhận đơn hàng thành công - " + orderCode;
        
        // Format số tiền
        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        String formattedAmount = currencyFormat.format(totalAmount);
        
        // Format ngày tháng
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        String formattedDate = orderDate.format(dateFormatter);
        
        // Tạo HTML email body
        StringBuilder htmlBody = new StringBuilder();
        htmlBody.append("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background: linear-gradient(135deg, #0ea5e9, #0284c7); color: white; padding: 30px; text-align: center; border-radius: 10px 10px 0 0; }
                    .content { background: #f9fafb; padding: 30px; border-radius: 0 0 10px 10px; }
                    .order-info { background: white; padding: 20px; border-radius: 8px; margin: 20px 0; }
                    .order-item { padding: 10px; border-bottom: 1px solid #eee; }
                    .order-item:last-child { border-bottom: none; }
                    .total { font-size: 18px; font-weight: bold; color: #0ea5e9; margin-top: 15px; }
                    .license-section { background: #f0fdf4; border: 2px solid #10b981; padding: 20px; border-radius: 8px; margin: 20px 0; }
                    .license-item { background: white; padding: 12px; margin: 8px 0; border-radius: 6px; border-left: 4px solid #10b981; }
                    .license-key { font-family: 'Courier New', monospace; font-size: 14px; font-weight: bold; color: #059669; word-break: break-all; }
                    .license-product { font-weight: 600; color: #111827; margin-bottom: 4px; }
                    .footer { text-align: center; margin-top: 30px; color: #666; font-size: 14px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>🎉 Đặt hàng thành công!</h1>
                    </div>
                    <div class="content">
                        <p>Xin chào <strong>%s</strong>,</p>
                        <p>Cảm ơn bạn đã đặt hàng tại <strong>BanHangRong</strong>!</p>
                        
                        <div class="order-info">
                            <h2>Thông tin đơn hàng</h2>
                            <p><strong>Mã đơn hàng:</strong> %s</p>
                            <p><strong>Ngày đặt hàng:</strong> %s</p>
                            <p><strong>Trạng thái:</strong> <span style="color: #10b981; font-weight: bold;">Đã hoàn thành</span></p>
                            
                            <h3 style="margin-top: 20px;">Chi tiết sản phẩm:</h3>
            """.formatted(customerName, orderCode, formattedDate));
        
        // Thêm danh sách sản phẩm
        for (OrderItemInfo item : orderItems) {
            htmlBody.append(String.format("""
                            <div class="order-item">
                                <p><strong>%s</strong></p>
                                <p>Số lượng: %d x %s</p>
                            </div>
                """, item.getProductName(), item.getQuantity(), currencyFormat.format(item.getPrice())));
        }
        
        htmlBody.append(String.format("""
                            <div class="total">
                                <p>Tổng tiền: %s</p>
                            </div>
                        </div>
            """, formattedAmount));
        
        // Thêm phần license keys nếu có
        if (licenseKeys != null && !licenseKeys.isEmpty()) {
            htmlBody.append("""
                        <div class="license-section">
                            <h2 style="color: #059669; margin-top: 0;">🔑 License Keys của bạn</h2>
                            <p style="margin-bottom: 15px;">Dưới đây là các license keys cho đơn hàng của bạn:</p>
            """);
            
            int sequenceNumber = 1;
            for (LicenseKeyInfo license : licenseKeys) {
                htmlBody.append(String.format("""
                            <div class="license-item">
                                <div class="license-product">%d. %s</div>
                                <div class="license-key">%s</div>
                                <div style="font-size: 12px; color: #6b7280; margin-top: 4px;">
                                    Trạng thái: <span style="color: %s; font-weight: 600;">%s</span>
                                </div>
                            </div>
                """, 
                    sequenceNumber++,
                    license.getProductName() != null ? license.getProductName() : "Product",
                    license.getLicenseKey(),
                    license.getIsActive() ? "#059669" : "#dc2626",
                    license.getIsActive() ? "Active" : "Inactive"
                ));
            }
            
            htmlBody.append("""
                            <p style="margin-top: 15px; font-size: 14px; color: #374151;">
                                <strong>Lưu ý:</strong> Vui lòng lưu lại các license keys này. Bạn có thể xem lại chúng trong trang "Orders History" của tài khoản.
                            </p>
                        </div>
            """);
        }
        
        htmlBody.append("""
                        <p>Đơn hàng của bạn đã được xử lý thành công. Chúng tôi sẽ gửi thông tin chi tiết về việc giao hàng trong thời gian sớm nhất.</p>
                        
                        <p>Nếu bạn có bất kỳ câu hỏi nào, vui lòng liên hệ với chúng tôi qua email: <a href="mailto:bonhoangncd@gmail.com">bonhoangncd@gmail.com</a></p>
                        
                        <div class="footer">
                            <p>Trân trọng,<br><strong>Đội ngũ BanHangRong</strong></p>
                            <p>© 2025 BanHangRong. All rights reserved.</p>
                        </div>
                    </div>
                </div>
            </body>
            </html>
            """);
        
        Email email = new Email(toEmail, subject, htmlBody.toString());
        // Gửi email HTML
        sendHtmlEmail(email);
    }
    
    /**
     * Gửi email HTML
     */
    private void sendHtmlEmail(Email email) {
        System.out.println("=== EMAIL SERVICE: Starting to send HTML email ===");
        System.out.println("To: " + email.getToEmail());
        System.out.println("Subject: " + email.getSubject());

        if (mailSender == null) {
            System.err.println("❌ ERROR: JavaMailSender is not configured. Email not sent to: " + email.getToEmail());
            System.err.println("Please check application.properties for email configuration");
            return;
        }
        
        MimeMessage message = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom("Ban Hang Rong <bonhoangncd@gmail.com>");
            helper.setTo(email.getToEmail());
            helper.setSubject(email.getSubject());
            helper.setText(email.getBody(), true); // true = HTML

            System.out.println("📧 Sending HTML email...");
            mailSender.send(message);
            System.out.println("✅ HTML email sent successfully to: " + email.getToEmail());

        } catch (MessagingException e) {
            System.err.println("❌ ERROR sending HTML email: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Error sending HTML email: " + e.getMessage(), e);
        } catch (Exception e) {
            System.err.println("❌ UNEXPECTED ERROR: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Unexpected error while sending HTML email: " + e.getMessage(), e);
        }
    }
    
    /**
     * Class để chứa thông tin sản phẩm trong đơn hàng
     */
    public static class OrderItemInfo {
        private String productName;
        private Integer quantity;
        private BigDecimal price;
        
        public OrderItemInfo(String productName, Integer quantity, BigDecimal price) {
            this.productName = productName;
            this.quantity = quantity;
            this.price = price;
        }
        
        public String getProductName() { return productName; }
        public Integer getQuantity() { return quantity; }
        public BigDecimal getPrice() { return price; }
    }
    
    /**
     * Class để chứa thông tin license key
     */
    public static class LicenseKeyInfo {
        private String licenseKey;
        private String productName;
        private Boolean isActive;
        
        public LicenseKeyInfo(String licenseKey, String productName, Boolean isActive) {
            this.licenseKey = licenseKey;
            this.productName = productName;
            this.isActive = isActive;
        }
        
        public String getLicenseKey() { return licenseKey; }
        public String getProductName() { return productName; }
        public Boolean getIsActive() { return isActive; }
    }
}
