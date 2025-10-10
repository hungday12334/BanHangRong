package banhangrong.su25.Controller;

import banhangrong.su25.Entity.Users;
import banhangrong.su25.Repository.UsersRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/debug")
public class DebugController {

    @Autowired
    private UsersRepository usersRepository;

    @GetMapping("/users")
    public List<Users> getAllUsers() {
        System.out.println("=== DEBUG: Getting all users from database ===");
        List<Users> users = usersRepository.findAll();
        System.out.println("=== DEBUG: Found " + users.size() + " users ===");
        for (Users user : users) {
            System.out.println("User: " + user.getUsername() + ", Type: " + user.getUserType() + ", Active: " + user.getIsActive());
        }
        return users;
    }

    @GetMapping("/user")
    public Object getUserByUsername(@RequestParam String username) {
        System.out.println("=== DEBUG: Looking for user: " + username + " ===");
        Optional<Users> user = usersRepository.findByUsername(username);
        if (user.isPresent()) {
            Users u = user.get();
            System.out.println("=== DEBUG: User found: " + u.getUsername() + ", Type: " + u.getUserType() + ", Active: " + u.getIsActive() + " ===");
            return u;
        } else {
            System.out.println("=== DEBUG: User not found: " + username + " ===");
            return "User not found: " + username;
        }
    }

    @GetMapping("/test-db")
    public String testDatabase() {
        try {
            long count = usersRepository.count();
            return "Database connection OK. Total users: " + count;
        } catch (Exception e) {
            return "Database connection failed: " + e.getMessage();
        }
    }
}
