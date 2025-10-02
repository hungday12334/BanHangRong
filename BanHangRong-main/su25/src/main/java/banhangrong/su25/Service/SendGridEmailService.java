package banhangrong.su25.Service;

import com.sendgrid.*;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class SendGridEmailService {

    @Value("${sendgrid.api.key:}")
    private String sendGridApiKey;

    @Value("${sendgrid.from.email:noreply@banhangrong.com}")
    private String fromEmail;

    @Value("${sendgrid.from.name:BanHangRong}")
    private String fromName;

    public boolean sendPasswordResetEmail(String toEmail, String resetToken) {
        try {
            // Kiểm tra API key
            if (sendGridApiKey == null || sendGridApiKey.isEmpty() || sendGridApiKey.contains("YOUR_SENDGRID")) {
                System.err.println("❌ SendGrid API key chưa được cấu hình!");
                return false;
            }

            String resetUrl = "http://localhost:8080/reset-password?token=" + resetToken;
            
            // Tạo email
            Email from = new Email(fromEmail, fromName);
            Email to = new Email(toEmail);
            String subject = "🔐 Đặt lại mật khẩu - BanHangRong";
            
            // Nội dung email HTML đẹp
            String htmlContent = createResetPasswordEmailTemplate(resetUrl, toEmail);
            Content content = new Content("text/html", htmlContent);
            
            Mail mail = new Mail(from, subject, to, content);

            // Gửi email
            SendGrid sg = new SendGrid(sendGridApiKey);
            Request request = new Request();
            
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());
            
            Response response = sg.api(request);
            
            if (response.getStatusCode() >= 200 && response.getStatusCode() < 300) {
                System.out.println("✅ Email đặt lại mật khẩu đã được gửi đến: " + toEmail);
                System.out.println("🔗 Reset URL: " + resetUrl);
                return true;
            } else {
                System.err.println("❌ Lỗi gửi email SendGrid: " + response.getStatusCode());
                System.err.println("Response: " + response.getBody());
                return false;
            }
            
        } catch (IOException ex) {
            System.err.println("❌ Lỗi khi gửi email qua SendGrid: " + ex.getMessage());
            return false;
        }
    }

    public boolean sendWelcomeEmail(String toEmail, String username) {
        try {
            if (sendGridApiKey == null || sendGridApiKey.isEmpty() || sendGridApiKey.contains("YOUR_SENDGRID")) {
                System.out.println("=== EMAIL CHÀO MỪNG (SENDGRID TEST MODE) ===");
                System.out.println("To: " + toEmail);
                System.out.println("Username: " + username);
                System.out.println("===========================================");
                return true;
            }

            Email from = new Email(fromEmail, fromName);
            Email to = new Email(toEmail);
            String subject = "🎉 Chào mừng đến với BanHangRong!";
            
            String htmlContent = createWelcomeEmailTemplate(username);
            Content content = new Content("text/html", htmlContent);
            
            Mail mail = new Mail(from, subject, to, content);

            SendGrid sg = new SendGrid(sendGridApiKey);
            Request request = new Request();
            
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());
            
            Response response = sg.api(request);
            
            if (response.getStatusCode() >= 200 && response.getStatusCode() < 300) {
                System.out.println("✅ Email chào mừng đã được gửi đến: " + toEmail);
                return true;
            } else {
                System.err.println("❌ Lỗi gửi email chào mừng: " + response.getStatusCode());
                return false;
            }
            
        } catch (IOException ex) {
            System.err.println("❌ Lỗi khi gửi email chào mừng: " + ex.getMessage());
            return false;
        }
    }

    private String createResetPasswordEmailTemplate(String resetUrl, String email) {
        return "<!DOCTYPE html>" +
            "<html>" +
            "<head>" +
                "<meta charset=\"UTF-8\">" +
                "<style>" +
                    "body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }" +
                    ".container { max-width: 600px; margin: 0 auto; padding: 20px; }" +
                    ".header { background: #007bff; color: white; padding: 20px; text-align: center; border-radius: 8px 8px 0 0; }" +
                    ".content { background: #f8f9fa; padding: 30px; border-radius: 0 0 8px 8px; }" +
                    ".button { display: inline-block; background: #28a745; color: white; padding: 12px 30px; text-decoration: none; border-radius: 5px; margin: 20px 0; }" +
                    ".footer { text-align: center; margin-top: 20px; font-size: 12px; color: #666; }" +
                    ".warning { background: #fff3cd; border: 1px solid #ffeaa7; padding: 15px; border-radius: 5px; margin: 20px 0; }" +
                "</style>" +
            "</head>" +
            "<body>" +
                "<div class=\"container\">" +
                    "<div class=\"header\">" +
                        "<h1>🔐 Đặt lại mật khẩu</h1>" +
                        "<p>BanHangRong - Nền tảng giao dịch license uy tín</p>" +
                    "</div>" +
                    "<div class=\"content\">" +
                        "<p>Xin chào,</p>" +
                        "<p>Bạn đã yêu cầu đặt lại mật khẩu cho tài khoản <strong>" + email + "</strong> tại BanHangRong.</p>" +
                        "<p style=\"text-align: center;\">" +
                            "<a href=\"" + resetUrl + "\" class=\"button\">🔗 Đặt lại mật khẩu ngay</a>" +
                        "</p>" +
                        "<div class=\"warning\">" +
                            "<strong>⚠️ LƯU Ý QUAN TRỌNG:</strong><br>" +
                            "• Link này chỉ dành cho bạn và sẽ hết hạn sau 24 giờ<br>" +
                            "• Không chia sẻ link này với bất kỳ ai<br>" +
                            "• Nếu bạn không yêu cầu đặt lại mật khẩu, vui lòng bỏ qua email này" +
                        "</div>" +
                        "<p>Nếu nút không hoạt động, bạn có thể copy link sau:</p>" +
                        "<p style=\"word-break: break-all; background: #e9ecef; padding: 10px; border-radius: 5px;\">" +
                            resetUrl +
                        "</p>" +
                        "<p>Trân trọng,<br><strong>Đội ngũ BanHangRong</strong></p>" +
                    "</div>" +
                    "<div class=\"footer\">" +
                        "<p>© 2024 BanHangRong. Mọi quyền được bảo lưu.</p>" +
                    "</div>" +
                "</div>" +
            "</body>" +
            "</html>";
    }

    private String createWelcomeEmailTemplate(String username) {
        return "<!DOCTYPE html>" +
            "<html>" +
            "<head>" +
                "<meta charset=\"UTF-8\">" +
                "<style>" +
                    "body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }" +
                    ".container { max-width: 600px; margin: 0 auto; padding: 20px; }" +
                    ".header { background: #28a745; color: white; padding: 20px; text-align: center; border-radius: 8px 8px 0 0; }" +
                    ".content { background: #f8f9fa; padding: 30px; border-radius: 0 0 8px 8px; }" +
                    ".feature { background: white; padding: 15px; margin: 10px 0; border-radius: 5px; border-left: 4px solid #007bff; }" +
                "</style>" +
            "</head>" +
            "<body>" +
                "<div class=\"container\">" +
                    "<div class=\"header\">" +
                        "<h1>🎉 Chào mừng đến với BanHangRong!</h1>" +
                    "</div>" +
                    "<div class=\"content\">" +
                        "<p>Xin chào <strong>" + username + "</strong>,</p>" +
                        "<p>Chào mừng bạn đến với BanHangRong - nền tảng giao dịch license uy tín!</p>" +
                        "<p>Tài khoản của bạn đã được tạo thành công. Bạn có thể bắt đầu:</p>" +
                        "<div class=\"feature\">💰 Mua các license phần mềm chất lượng</div>" +
                        "<div class=\"feature\">🏪 Bán license của bạn</div>" +
                        "<div class=\"feature\">👤 Quản lý tài khoản cá nhân</div>" +
                        "<div class=\"feature\">📊 Theo dõi giao dịch</div>" +
                        "<p>Nếu bạn có bất kỳ câu hỏi nào, vui lòng liên hệ với chúng tôi.</p>" +
                        "<p>Trân trọng,<br><strong>Đội ngũ BanHangRong</strong></p>" +
                    "</div>" +
                "</div>" +
            "</body>" +
            "</html>";
    }
}
