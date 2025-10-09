package banhangrong.su25.service;

import banhangrong.su25.Entity.Users;
import banhangrong.su25.Repository.UsersRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserProfileService {

    @Autowired
    private UsersRepository usersRepository;

    public Users getUserById(Long userId) {
        return usersRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
    }

    public Users getSellerProfile(Long sellerId) {
        Users user = getUserById(sellerId);
        if (!user.isSeller()) {
            throw new RuntimeException("User is not a seller");
        }
        return user;
    }

    public Users updateSellerProfile(Long sellerId, Users updatedUser) {
        Users existingUser = getSellerProfile(sellerId);

        // Chỉ cập nhật các field được phép
        if (updatedUser.getUsername() != null) {
            existingUser.setUsername(updatedUser.getUsername());
        }
        if (updatedUser.getPhoneNumber() != null) {
            existingUser.setPhoneNumber(updatedUser.getPhoneNumber());
        }
        if (updatedUser.getGender() != null) {
            existingUser.setGender(updatedUser.getGender());
        }
        if (updatedUser.getBirthDate() != null) {
            existingUser.setBirthDate(updatedUser.getBirthDate());
        }
        if (updatedUser.getAvatarUrl() != null) {
            existingUser.setAvatarUrl(updatedUser.getAvatarUrl());
        }

        return usersRepository.save(existingUser);
    }

    public Optional<Users> getUserByEmail(String email) {
        return usersRepository.findByEmail(email);
    }
}