package banhangrong.su25.service;

import banhangrong.su25.Entity.Users;
import banhangrong.su25.Repository.UsersRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserProfileService {

    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public Users getUserById(Long userId) {
        return usersRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
    }

    public Users getSellerProfile(Long sellerId) {
        Users user = getUserById(sellerId);
        // Note: isSeller() method may not exist, simplified version
        return user;
    }

    public Users updateSellerProfile(Long sellerId, Users updatedUser) {
        try {
            System.out.println("=== UPDATING SELLER PROFILE ===");
            System.out.println("Seller ID: " + sellerId);

            Users existingUser = getSellerProfile(sellerId);
            System.out.println("Current username: " + existingUser.getUsername());
            System.out.println("Current phone: " + existingUser.getPhoneNumber());
            System.out.println("Current gender: " + existingUser.getGender());
            System.out.println("Current birth date: " + existingUser.getBirthDate());

            // Chỉ cập nhật các field được phép
            if (updatedUser.getUsername() != null) {
                existingUser.setUsername(updatedUser.getUsername());
                System.out.println("New username: " + updatedUser.getUsername());
            }
            if (updatedUser.getPhoneNumber() != null) {
                existingUser.setPhoneNumber(updatedUser.getPhoneNumber());
                System.out.println("New phone: " + updatedUser.getPhoneNumber());
            }
            if (updatedUser.getGender() != null) {
                existingUser.setGender(updatedUser.getGender());
                System.out.println("New gender: " + updatedUser.getGender());
            }
            if (updatedUser.getBirthDate() != null) {
                existingUser.setBirthDate(updatedUser.getBirthDate());
                System.out.println("New birth date: " + updatedUser.getBirthDate());
            }

            Users savedUser = usersRepository.save(existingUser);
            System.out.println("✅ Profile saved successfully");
            System.out.println("Saved username: " + savedUser.getUsername());
            System.out.println("Saved phone: " + savedUser.getPhoneNumber());
            System.out.println("Saved gender: " + savedUser.getGender());
            System.out.println("Saved birth date: " + savedUser.getBirthDate());

            return savedUser;

        } catch (Exception e) {
            System.out.println("❌ Error in updateSellerProfile: " + e.getMessage());
            throw e;
        }
    }

    // === THÊM METHOD UPDATE AVATAR ===
    public void updateAvatar(Long sellerId, String avatarUrl) {
        try {
            System.out.println("Updating avatar for seller " + sellerId + " to: " + avatarUrl);
            Users existingUser = getSellerProfile(sellerId);
            existingUser.setAvatarUrl(avatarUrl);
            usersRepository.save(existingUser);
            System.out.println("Avatar updated successfully");
        } catch (Exception e) {
            System.out.println("Error updating avatar: " + e.getMessage());
            throw e;
        }
    }

    // === THÊM METHODS XỬ LÝ MẬT KHẨU ===

    public void changePassword(Long sellerId, String newPassword) {
        try {
            System.out.println("Changing password for seller: " + sellerId);

            Users existingUser = getSellerProfile(sellerId);

            // Mã hóa mật khẩu mới (sử dụng BCrypt)
            String encryptedPassword = passwordEncoder.encode(newPassword);
            existingUser.setPassword(encryptedPassword);

            usersRepository.save(existingUser);
            System.out.println("✅ Password changed successfully");

        } catch (Exception e) {
            System.out.println("❌ Error in changePassword: " + e.getMessage());
            throw e;
        }
    }

    public boolean verifyPassword(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }

    public Optional<Users> getUserByEmail(String email) {
        return usersRepository.findByEmail(email);
    }


}
