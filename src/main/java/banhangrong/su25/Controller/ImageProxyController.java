package banhangrong.su25.Controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Controller;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.Comparator;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Simple image proxy that downloads external images (e.g. imgbb) and caches them on disk.
 * Usage: /img/proxy?url=<encoded-image-url>
 * This improves perceived load by serving images from the app host, allowing CDN/cache-control
 * and reducing client-side blocked DNS/SSL handshakes to imgbb on every request.
 */
@Controller
public class ImageProxyController {

    @Value("${image.proxy.cache-dir:./image-cache}")
    private String cacheDirProp;

    @Value("${image.proxy.max-files:200}")
    private int maxFiles;

    private Path cacheDir;
    private final AtomicInteger fileCounter = new AtomicInteger(0);

    @PostConstruct
    public void init() throws IOException {
        cacheDir = Path.of(cacheDirProp).toAbsolutePath();
        if (!Files.exists(cacheDir)) Files.createDirectories(cacheDir);
        // initialize counter
        try {
            long count = Files.list(cacheDir).count();
            fileCounter.set((int)Math.min(count, Integer.MAX_VALUE));
        } catch (Exception ignored) {}
    }

    @GetMapping("/img/proxy")
    @ResponseBody
    public ResponseEntity<byte[]> proxy(@RequestParam("url") String urlStr, HttpServletRequest req) {
        if (urlStr == null || urlStr.isBlank()) return ResponseEntity.badRequest().build();

        // whitelist basic hosts optionally (for safety). If you want only imgbb, uncomment check below.
        // if (!urlStr.contains("imgbb.com") && !urlStr.contains("i.ibb.co") && !urlStr.contains("i.ibb")) {
        //     return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
        // }

        try {
            URI uri = URI.create(urlStr);
            String fname = Integer.toHexString(Objects.hash(uri.toString())) + getExtension(uri.getPath());
            Path cached = cacheDir.resolve(fname);
            if (Files.exists(cached)) {
                Resource res = new UrlResource(cached.toUri());
                String contentType = Files.probeContentType(cached);
                byte[] data = StreamUtils.copyToByteArray(res.getInputStream());
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_TYPE, contentType == null ? MediaType.APPLICATION_OCTET_STREAM_VALUE : contentType)
                        .cacheControl(CacheControl.maxAge(Duration.ofDays(30)).cachePublic())
                        .body(data);
            }

            // download
            URL url = uri.toURL();
            URLConnection conn = url.openConnection();
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(10000);
            String contentType = conn.getContentType();
            try (InputStream in = conn.getInputStream()) {
                Files.copy(in, cached, StandardCopyOption.REPLACE_EXISTING);
            }

            fileCounter.incrementAndGet();
            evictIfNeeded();

            byte[] data = Files.readAllBytes(cached);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, contentType == null ? MediaType.APPLICATION_OCTET_STREAM_VALUE : contentType)
                    .cacheControl(CacheControl.maxAge(Duration.ofDays(30)).cachePublic())
                    .body(data);

        } catch (IllegalArgumentException iae) {
            return ResponseEntity.badRequest().build();
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
        }
    }

    private static String getExtension(String path) {
        if (path == null) return "";
        int i = path.lastIndexOf('.');
        if (i == -1) return "";
        String ext = path.substring(i);
        // sanitize
        if (ext.length() > 8) return "";
        return ext.replaceAll("[^a-zA-Z0-9.]", "");
    }

    private void evictIfNeeded() {
        try {
            long count = Files.list(cacheDir).count();
            if (count <= maxFiles) return;
            // delete oldest files until under limit
            Files.list(cacheDir)
                    .sorted(Comparator.comparingLong(p -> p.toFile().lastModified()))
                    .limit(count - maxFiles)
                    .forEach(p -> {
                        try { Files.deleteIfExists(p); } catch (Exception ignored) {}
                    });
        } catch (Exception ignored) {}
    }

    // periodic cleanup to ensure size remains bounded
    @Scheduled(fixedDelayString = "${image.proxy.cleanup-ms:3600000}")
    public void scheduledCleanup() {
        evictIfNeeded();
    }
}
