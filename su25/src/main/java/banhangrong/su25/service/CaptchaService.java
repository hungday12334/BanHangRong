package banhangrong.su25.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

@Service
public class CaptchaService {

    @Value("${captcha.secret-key}")
    private String captchaSecretKey;

    private final WebClient webClient;

    public CaptchaService() {
        this.webClient = WebClient.builder()
                .baseUrl("https://www.google.com/recaptcha/api")
                .build();
    }

    public boolean verifyCaptcha(String captchaResponse) {
        if (captchaResponse == null || captchaResponse.trim().isEmpty()) {
            return false;
        }

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = webClient.post()
                    .uri("/siteverify")
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .bodyValue("secret=" + captchaSecretKey + "&response=" + captchaResponse)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            return response != null && Boolean.TRUE.equals(response.get("success"));
        } catch (Exception e) {
            System.err.println("CAPTCHA verification error: " + e.getMessage());
            return false;
        }
    }
}
