package banhangrong.su25.Controller;

import org.springframework.core.io.ClassPathResource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.time.Duration;

@RestController
public class FaviconController {

    @GetMapping("/favicon.ico")
    public ResponseEntity<byte[]> favicon() throws IOException {
        // Reuse provided PNG logo as favicon
        ClassPathResource img = new ClassPathResource("static/img/kua654ms.png");
        byte[] bytes = img.getInputStream().readAllBytes();
        return ResponseEntity
                .ok()
                .contentType(MediaType.IMAGE_PNG)
                .cacheControl(CacheControl.maxAge(Duration.ofDays(30)))
                .body(bytes);
    }
}
