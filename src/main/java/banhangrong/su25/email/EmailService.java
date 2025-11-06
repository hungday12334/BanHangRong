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
                                          List<OrderItemInfo> orderItems) {
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
            """, formattedAmount));
        
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
}
