package banhangrong.su25.Controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Base64;
import java.util.Map;

@RestController
@RequestMapping("/api/uploads")
public class UploadController {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${imgbb.api.key:}")
    private String imgbbApiKey;
    // Also allow IMGBB_API_KEY from .env (simple profile imports .env as properties)
    @Value("${IMGBB_API_KEY:}")
    private String imgbbApiKeyAlt;

    @PostMapping(path = "/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam(name = "expiration", required = false) Integer expiration
    ) throws IOException {
        if (file.isEmpty()) return ResponseEntity.badRequest().body(Map.of("error", "empty file"));
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return ResponseEntity.badRequest().body(Map.of("error", "file must be an image"));
        }
        // Require Imgbb key; no local fallback
        String effectiveKey = (imgbbApiKey != null && !imgbbApiKey.isBlank()) ? imgbbApiKey : (imgbbApiKeyAlt != null ? imgbbApiKeyAlt : "");
        if (effectiveKey.isBlank()) {
            return ResponseEntity.status(501).body(Map.of("error", "imgbb api key not configured"));
        }
        try {
            String base64 = Base64.getEncoder().encodeToString(file.getBytes());
            String apiUrl = "https://api.imgbb.com/1/upload?key=" + effectiveKey;
            if (expiration != null && expiration > 0) {
                apiUrl += "&expiration=" + expiration;
            }
            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("image", base64);
            String orig = file.getOriginalFilename();
            String name = (orig != null && !orig.isBlank()) ? orig : ("img_" + System.currentTimeMillis());
            form.add("name", name);

            RestTemplate rt = new RestTemplate();
            String resp = rt.postForObject(apiUrl, form, String.class);
            if (resp == null || resp.isBlank()) {
                return ResponseEntity.internalServerError().body(Map.of("error", "empty response from imgbb"));
            }
            JsonNode root = objectMapper.readTree(resp);
            boolean success = root.path("success").asBoolean(false);
            if (!success) {
                int status = root.path("status").asInt(500);
                String msg = root.path("error").path("message").asText("imgbb upload failed");
                return ResponseEntity.status(status).body(Map.of("error", msg));
            }
            String url = root.path("data").path("url").asText();
            if (url == null || url.isBlank()) {
                url = root.path("data").path("display_url").asText("");
            }
            if (url == null || url.isBlank()) {
                return ResponseEntity.internalServerError().body(Map.of("error", "imgbb response without url"));
            }
            return ResponseEntity.ok(Map.of("url", url));
        } catch (Exception ex) {
            return ResponseEntity.internalServerError().body(Map.of("error", "imgbb upload error"));
        }
    }
}
