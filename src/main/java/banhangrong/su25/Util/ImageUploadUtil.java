package banhangrong.su25.Util;

import org.springframework.web.multipart.MultipartFile;


import java.io.IOException;
import java.nio.file.*;
import java.util.Objects;

public class ImageUploadUtil {
    // 🧠 Hàm lưu file avatar
    public  String saveAvatar(MultipartFile file, String username) throws IOException {
        if (file == null || file.isEmpty()) {
            return "";
        }

        // Lấy phần mở rộng của file
        String originalName = Objects.requireNonNull(file.getOriginalFilename());
        String extension = originalName.substring(originalName.lastIndexOf(".")).toLowerCase();

        // Tạo tên file theo định dạng: username + .ext
        String fileName = username + extension;

        // Đường dẫn lưu file
        Path uploadDir = Paths.get(System.getProperty("user.dir"), "uploads", "avatar");

        if (!Files.exists(uploadDir)) {
            Files.createDirectories(uploadDir);
        }

        // Ghi file vào thư mục
        Path filePath = uploadDir.resolve(fileName);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        // Trả về tên file để lưu vào database
        return fileName;
    }
}
