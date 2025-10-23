package banhangrong.su25.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class CaptchaService {

    @Value("${captcha.secret-key}")
    private String captchaSecretKey;

    @SuppressWarnings("unused")
    private final WebClient webClient;

    public CaptchaService() {
        this.webClient = WebClient.builder()
                .baseUrl("https://www.google.com/recaptcha/api")
                .build();
    }

    public boolean verifyCaptcha(String captchaResponse) {
        // ===== TẠM THỜI TẮT CAPTCHA =====
        // TODO: Bật lại captcha khi cần thiết
        System.out.println("CAPTCHA: DISABLED - Bypassing verification");
        return true;
        
        /* COMMENT OUT - Code gốc để bật lại sau
        if (captchaResponse == null || captchaResponse.trim().isEmpty()) {
            System.out.println("CAPTCHA: Empty response");
            return false;
        }

        try {
            System.out.println("CAPTCHA: Verifying with secret key: " + captchaSecretKey.substring(0, 10) + "...");
            System.out.println("CAPTCHA: Response token: " + captchaResponse.substring(0, Math.min(20, captchaResponse.length())) + "...");
            
            @SuppressWarnings("unchecked")
            Map<String, Object> response = webClient.post()
                    .uri("/siteverify")
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .bodyValue("secret=" + captchaSecretKey + "&response=" + captchaResponse)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            System.out.println("CAPTCHA: Google response: " + response);
            
            boolean isValid = response != null && Boolean.TRUE.equals(response.get("success"));
            System.out.println("CAPTCHA: Validation result: " + isValid);
            
            return isValid;
        } catch (Exception e) {
            System.err.println("CAPTCHA verification error: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
        */
    }
}
