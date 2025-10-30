package banhangrong.su25.Util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.multipart.MultipartFile;

public class Validation {

    public  boolean isPhoneValid(String phone){
        String regex = "^(03|05|07|08|09)\\d{8}$";
        return phone != null && phone.matches(regex);
    }

    public  boolean isPasswordValid(String password){
        return password != null && password.length() >= 6;
    }
    public  String hashPassword(String password){
        PasswordEncoder encoder= new BCryptPasswordEncoder();
        return encoder.encode(password);
    }
    public boolean isImageFileValid(MultipartFile file) {
        // 1️⃣ Nếu không có file hoặc file trống
        if (file == null || file.isEmpty()) {
            return false;
        }

        // 2️⃣ Kiểm tra MIME type
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return false;
        }

        return true;
    }
    public  boolean hasSpace(String input) {
        if (input == null) return false; // tránh NullPointerException
        return input.contains(" ");
    }
}
