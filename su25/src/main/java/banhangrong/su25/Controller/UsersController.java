package banhangrong.su25.Controller;

import banhangrong.su25.Entity.Users;
import banhangrong.su25.Repository.UsersRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/users")
public class UsersController {

    private final UsersRepository usersRepository;

    public UsersController(UsersRepository usersRepository) {
        this.usersRepository = usersRepository;
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getUser(@PathVariable("id") Long id) {
        Optional<Users> opt = usersRepository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "User not found"));
        Users u = opt.get();
        Map<String, Object> dto = new HashMap<>();
        dto.put("userId", u.getUserId());
        dto.put("username", u.getUsername());
        dto.put("email", u.getEmail());
        dto.put("phoneNumber", u.getPhoneNumber());
        dto.put("avatarUrl", u.getAvatarUrl());
        dto.put("userType", u.getUserType());
        return ResponseEntity.ok(dto);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(@PathVariable("id") Long id, @RequestBody Map<String, Object> payload) {
        Optional<Users> opt = usersRepository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "User not found"));
        Users u = opt.get();

        // Only allow safe fields to be updated
        if (payload.containsKey("username")) {
            Object v = payload.get("username");
            u.setUsername(v != null ? v.toString() : null);
        }
        if (payload.containsKey("email")) {
            Object v = payload.get("email");
            u.setEmail(v != null ? v.toString() : null);
        }
        if (payload.containsKey("phoneNumber")) {
            Object v = payload.get("phoneNumber");
            u.setPhoneNumber(v != null ? v.toString() : null);
        }
        if (payload.containsKey("avatarUrl")) {
            Object v = payload.get("avatarUrl");
            u.setAvatarUrl(v != null ? v.toString() : null);
        }

        usersRepository.save(u);

        Map<String, Object> dto = new HashMap<>();
        dto.put("userId", u.getUserId());
        dto.put("username", u.getUsername());
        dto.put("email", u.getEmail());
        dto.put("phoneNumber", u.getPhoneNumber());
        dto.put("avatarUrl", u.getAvatarUrl());
        dto.put("userType", u.getUserType());
        return ResponseEntity.ok(dto);
    }
}
