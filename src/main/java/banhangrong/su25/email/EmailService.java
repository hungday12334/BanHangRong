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
        if (mailSender == null) {
            System.err.println("Warning: JavaMailSender is not configured. Email not sent to: " + email.getToEmail());
            return;
        }
        
        MimeMessage message = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setFrom("Dormitory@gmail.com");
            helper.setTo(email.getToEmail());
            helper.setSubject(email.getSubject());
            helper.setText(email.getBody(), false);

            mailSender.send(message);

        } catch (MessagingException e) {
            throw new RuntimeException("Lỗi khi gửi email: " + e.getMessage());
        }
    }

    // Gửi email reset password (AuthController đang dùng)
    public void sendPasswordResetEmail(String to, String token) {
        String subject = "Password Reset Request";
        String resetLink = "http://localhost:8080/reset-password?token=" + token;
        String body = "Nhấn vào link để đặt lại mật khẩu: " + resetLink;

        Email email = new Email(to, subject, body);
        sendEmail(email);
    }
}
