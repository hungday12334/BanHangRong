package banhangrong.su25.email;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

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
}
