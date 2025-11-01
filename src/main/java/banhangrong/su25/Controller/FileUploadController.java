package banhangrong.su25.ontroller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/upload")
public class FileUploadController {

    @Value("${file.upload-dir:uploads/chat}")
    private String uploadDir;

    private static final long MAX_IMAGE_SIZE = 5 * 1024 * 1024; // 5MB
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

    /**
     * Upload image for chat
     */
    @PostMapping("/image")
    public ResponseEntity<?> uploadImage(@RequestParam("file") MultipartFile file) {
        try {
            // Validate file
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(createErrorResponse("File is empty"));
            }

            // Check file size
            if (file.getSize() > MAX_IMAGE_SIZE) {
                return ResponseEntity.badRequest().body(createErrorResponse("Image size must be less than 5MB"));
            }

            // Check file type
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                return ResponseEntity.badRequest().body(createErrorResponse("File must be an image"));
            }

            // Save file
            String fileUrl = saveFile(file, "images");

            return ResponseEntity.ok(createSuccessResponse(fileUrl, file.getOriginalFilename(), file.getSize(), "image"));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to upload image: " + e.getMessage()));
        }
    }

    /**
     * Upload file (PDF, DOC, etc.) for chat
     */
    @PostMapping("/file")
    public ResponseEntity<?> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            // Validate file
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(createErrorResponse("File is empty"));
            }

            // Check file size
            if (file.getSize() > MAX_FILE_SIZE) {
                return ResponseEntity.badRequest().body(createErrorResponse("File size must be less than 10MB"));
            }

            // Check file extension
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null || !isAllowedFileType(originalFilename)) {
                return ResponseEntity.badRequest().body(createErrorResponse("File type not allowed. Allowed: PDF, DOC, DOCX, TXT, ZIP, RAR"));
            }

            // Save file
            String fileUrl = saveFile(file, "files");

            return ResponseEntity.ok(createSuccessResponse(fileUrl, file.getOriginalFilename(), file.getSize(), "file"));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to upload file: " + e.getMessage()));
        }
    }

    /**
     * Save file to disk and return URL
     */
    private String saveFile(MultipartFile file, String subfolder) throws IOException {
        // Create upload directory if not exists
        Path uploadPath = Paths.get(uploadDir, subfolder);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // Generate unique filename
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String uniqueFilename = UUID.randomUUID().toString() + extension;

        // Save file
        Path filePath = uploadPath.resolve(uniqueFilename);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        // Return URL path
        return "/" + uploadDir + "/" + subfolder + "/" + uniqueFilename;
    }

    /**
     * Check if file type is allowed
     */
    private boolean isAllowedFileType(String filename) {
        String lowerFilename = filename.toLowerCase();
        return lowerFilename.endsWith(".pdf") ||
               lowerFilename.endsWith(".doc") ||
               lowerFilename.endsWith(".docx") ||
               lowerFilename.endsWith(".txt") ||
               lowerFilename.endsWith(".zip") ||
               lowerFilename.endsWith(".rar");
    }

    /**
     * Create success response
     */
    private Map<String, Object> createSuccessResponse(String fileUrl, String filename, long size, String type) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("fileUrl", fileUrl);
        response.put("filename", filename);
        response.put("size", size);
        response.put("type", type);
        return response;
    }

    /**
     * Create error response
     */
    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("error", message);
        return response;
    }
}

