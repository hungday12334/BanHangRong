package banhangrong.su25.Controller;

import banhangrong.su25.Entity.Users;
import banhangrong.su25.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/admin/user")
public class UserManagement {
    @Autowired
    private UserService userService;

    @GetMapping("/create")
    public String showCreateScreen(Model model) {
        return "admin/user-creation";
    }

    @PostMapping("/create")
    public String createUser(HttpServletRequest request, Model model) {
        LocalDateTime now = LocalDateTime.now();
        Users user = new Users();
        user.setUsername(request.getParameter("username"));
        user.setEmail(request.getParameter("email"));
        user.setPassword(request.getParameter("password"));
        user.setUserType(request.getParameter("userType"));
        user.setPhoneNumber(request.getParameter("phoneNumber"));
        user.setAvatarUrl(request.getParameter("avatarUrl"));
        user.setGender(request.getParameter("gender"));
        String birthDate = request.getParameter("birthDate");
        if(birthDate != null && !birthDate.isEmpty()){
            user.setBirthDate(LocalDate.parse(birthDate));
        }
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        if(userService.existsByUsername(user.getUsername()) || userService.existsByEmail(user.getEmail())){
            model.addAttribute("errorExist", "Username or Email already exists");
            model.addAttribute("user", user);
        }else {
            userService.save(user);
            model.addAttribute("success", "User created successfully");
        }
        return "admin/user-creation";
    }
    @GetMapping("/update")
    public String showUpdateForm(HttpServletRequest request, Model model) {
        String sId = request.getParameter("id");
        if(sId == null || sId.isEmpty()){
            model.addAttribute("error", "User not found");
            return "admin/user-management"; // hoặc trang khác phù hợp
        }
        Long id = Long.parseLong(sId);
        Users user = userService.findById(id);
        if (user == null) {
            model.addAttribute("error", "User not found");
            return "admin/user-management"; // hoặc trang khác phù hợp
        }

        // Đưa user vào model để Thymeleaf hiển thị lên form
        model.addAttribute("user", user);
        return "admin/user-update"; // tên file .html hiển thị form update
    }
}
