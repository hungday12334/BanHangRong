package banhangrong.su25.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private SendGridEmailService sendGridEmailService;

    @Value("${spring.mail.username:your-email@gmail.com}")
    private String fromEmail;

    @Value("${email.provider:sendgrid}")
    private String emailProvider;

    public void sendPasswordResetEmail(String toEmail, String resetToken) {
        // Ưu tiên sử dụng SendGrid API
        if ("sendgrid".equalsIgnoreCase(emailProvider)) {
            boolean success = sendGridEmailService.sendPasswordResetEmail(toEmail, resetToken);
            if (success) {
                return; // Gửi thành công qua SendGrid
            }
            // Nếu SendGrid thất bại, fallback sang SMTP
            System.out.println("⚠️ SendGrid thất bại, chuyển sang SMTP fallback...");
        }
        
        // Fallback: Sử dụng SMTP truyền thống
        String resetUrl = "http://localhost:8080/reset-password?token=" + resetToken;
        
        try {
            // Kiểm tra cấu hình email
            if (fromEmail.contains("YOUR_EMAIL") || fromEmail.contains("your-email")) {
                throw new Exception("Email chưa được cấu hình đúng - vui lòng cập nhật spring.mail.username");
            }
            
            // Gửi email thực tế
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("🔐 Đặt lại mật khẩu - BanHangRong");
            
            message.setText(
                "Xin chào,\n\n" +
                "Bạn đã yêu cầu đặt lại mật khẩu cho tài khoản BanHangRong.\n\n" +
                "🔗 LINK XÁC NHẬN CHÍNH CHỦ:\n" +
                resetUrl + "\n\n" +
                "⚠️ LƯU Ý QUAN TRỌNG:\n" +
                "- Link này chỉ dành cho bạn và sẽ hết hạn sau 24 giờ\n" +
                "- Không chia sẻ link này với bất kỳ ai\n" +
                "- Click vào link để chuyển đến trang đổi mật khẩu mới\n\n" +
                "Nếu bạn không yêu cầu đặt lại mật khẩu, vui lòng bỏ qua email này.\n\n" +
                "Trân trọng,\n" +
                "Đội ngũ BanHangRong"
            );
            
            mailSender.send(message);
            System.out.println("✅ Email đặt lại mật khẩu đã được gửi đến: " + toEmail);
            System.out.println("🔗 Reset URL: " + resetUrl);
            
        } catch (Exception e) {
            System.err.println("❌ Lỗi khi gửi email: " + e.getMessage());
            
            // Fallback - hiển thị thông tin trong console và tạo file
            System.out.println("\n" + "=".repeat(80));
            System.out.println("📧 EMAIL RESET PASSWORD (FALLBACK MODE)");
            System.out.println("=".repeat(80));
            System.out.println("To: " + toEmail);
            System.out.println("Subject: 🔐 Đặt lại mật khẩu - BanHangRong");
            System.out.println("Reset Token: " + resetToken);
            System.out.println("Reset URL: " + resetUrl);
            System.out.println("=".repeat(80));
            System.out.println("💡 Hướng dẫn:");
            System.out.println("1. Copy Reset URL ở trên");
            System.out.println("2. Mở trình duyệt và paste URL");
            System.out.println("3. Đặt lại mật khẩu mới");
            System.out.println("4. Link này sẽ hết hạn sau 24 giờ");
            System.out.println("=".repeat(80) + "\n");
            
            // Tạo file reset link để dễ copy
            try {
                java.nio.file.Files.write(
                    java.nio.file.Paths.get("reset-password-link.txt"),
                    ("🔐 RESET PASSWORD LINK - BANHANGRONG\n" +
                     "=".repeat(50) + "\n" +
                     "Email: " + toEmail + "\n" +
                     "Token: " + resetToken + "\n" +
                     "Reset URL: " + resetUrl + "\n" +
                     "Generated at: " + java.time.LocalDateTime.now() + "\n" +
                     "Expires in: 24 hours\n" +
                     "=".repeat(50) + "\n" +
                     "Hướng dẫn:\n" +
                     "1. Copy URL ở trên\n" +
                     "2. Mở trình duyệt và paste URL\n" +
                     "3. Đặt lại mật khẩu mới\n" +
                     "4. Link sẽ hết hạn sau 24 giờ").getBytes()
                );
                System.out.println("📄 Reset link đã được lưu vào file: reset-password-link.txt");
            } catch (Exception fileError) {
                System.err.println("Không thể tạo file: " + fileError.getMessage());
            }
        }
    }

    public void sendWelcomeEmail(String toEmail, String username) {
        // Ưu tiên sử dụng SendGrid API
        if ("sendgrid".equalsIgnoreCase(emailProvider)) {
            boolean success = sendGridEmailService.sendWelcomeEmail(toEmail, username);
            if (success) {
                return; // Gửi thành công qua SendGrid
            }
            System.out.println("⚠️ SendGrid thất bại, chuyển sang SMTP fallback...");
        }
        
        try {
            // Kiểm tra xem có cấu hình email thực tế không
            if (fromEmail.contains("your-email") || fromEmail.contains("@gmail.com")) {
                // Chế độ test - chỉ in ra console
                System.out.println("=== EMAIL CHÀO MỪNG (TEST MODE) ===");
                System.out.println("To: " + toEmail);
                System.out.println("Subject: Chào mừng đến với BanHangRong!");
                System.out.println("Content:");
                System.out.println("Xin chào " + username + ",");
                System.out.println("Chào mừng bạn đến với BanHangRong - nền tảng giao dịch license uy tín!");
                System.out.println("Tài khoản của bạn đã được tạo thành công.");
                System.out.println("Bạn có thể bắt đầu:");
                System.out.println("- Mua các license phần mềm");
                System.out.println("- Bán license của bạn");
                System.out.println("- Quản lý tài khoản");
                System.out.println("Nếu bạn có bất kỳ câu hỏi nào, vui lòng liên hệ với chúng tôi.");
                System.out.println("Trân trọng,");
                System.out.println("Đội ngũ BanHangRong");
                System.out.println("=====================================");
                return;
            }
            
            // Chế độ production - gửi email thực tế
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("Chào mừng đến với BanHangRong!");
            
            message.setText(
                "Xin chào " + username + ",\n\n" +
                "Chào mừng bạn đến với BanHangRong - nền tảng giao dịch license uy tín!\n\n" +
                "Tài khoản của bạn đã được tạo thành công.\n\n" +
                "Bạn có thể bắt đầu:\n" +
                "- Mua các license phần mềm\n" +
                "- Bán license của bạn\n" +
                "- Quản lý tài khoản\n\n" +
                "Nếu bạn có bất kỳ câu hỏi nào, vui lòng liên hệ với chúng tôi.\n\n" +
                "Trân trọng,\n" +
                "Đội ngũ BanHangRong"
            );
            
            mailSender.send(message);
            System.out.println("Email chào mừng đã được gửi đến: " + toEmail);
        } catch (Exception e) {
            System.err.println("Lỗi khi gửi email chào mừng: " + e.getMessage());
            // Không throw exception để không làm crash ứng dụng
            System.out.println("=== EMAIL CHÀO MỪNG (FALLBACK MODE) ===");
            System.out.println("To: " + toEmail);
            System.out.println("Username: " + username);
            System.out.println("=======================================");
        }
    }
}
