package banhangrong.su25.Controller;

import banhangrong.su25.Entity.Users;
import banhangrong.su25.Util.ImageUploadUtil;
import banhangrong.su25.Util.Validation;
import banhangrong.su25.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

@Controller
@RequestMapping("/admin/user")
public class AdminUserManagement {
    @Autowired
    private UserService userService;

    @GetMapping("/create")
    public String showCreateScreen(Model model) {
        return "admin/user-creation";
    }

    @PostMapping("/create")
    public String createUser(HttpServletRequest request, Model model, RedirectAttributes redirectAttributes) {
        //Delare
        ImageUploadUtil imageUploadUtil = new ImageUploadUtil();
        Validation valid =  new Validation();
        LocalDateTime now = LocalDateTime.now();
        Users user = new Users();
        MultipartHttpServletRequest multipartRequest = (MultipartHttpServletRequest) request;

        //Get infor from font-end and set to entity
        user.setUsername(request.getParameter("username"));
        user.setEmail(request.getParameter("email"));
        user.setPassword(request.getParameter("password"));
        user.setUserType(request.getParameter("userType"));
        user.setPhoneNumber(request.getParameter("phoneNumber"));
        user.setAvatarUrl("");//Default null, if having image --> solving below
        user.setGender(request.getParameter("gender"));
        String birthDate = request.getParameter("birthDate");
        if (birthDate != null && !birthDate.isEmpty()) {
            user.setBirthDate(LocalDate.parse(birthDate));//from yyyy-MM-dd to LocalDate
        }
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        //Check valid username and email
        if (userService.existsByUsername(user.getUsername()) || userService.existsByEmail(user.getEmail())) {
            model.addAttribute("error", "Username or Email already exists");
            model.addAttribute("user", user);
            return "admin/user-creation";
        }

        //Check valid pasword
        if(!valid.isPasswordValid(user.getPassword())){
            model.addAttribute("error", "Password must be at least 6 characters long");
            model.addAttribute("user", user);
            return "admin/user-creation";
        }
        //Check valid phone
        if(user.getPhoneNumber()!=null&&!user.getPhoneNumber().isEmpty()){
            if(!valid.isPhoneValid(user.getPhoneNumber())){
                model.addAttribute("error", "Invalid phone number");
                model.addAttribute("user", user);
                return "admin/user-creation";
            }
        }
        // Solving if having image
        MultipartFile avatar = multipartRequest.getFile("avatarUrl");

        if (avatar != null && !avatar.isEmpty()) {

            // User entered an invalid file
            if (!valid.isImageFileValid(avatar)) {
                model.addAttribute("error", "This file is not an image");
                model.addAttribute("user", user);
                return "admin/user-creation";
            }

            // Save the image into static/img/avatar folder, naming follows role+id
            try {
                user.setAvatarUrl(imageUploadUtil.saveAvatar(avatar, user.getUsername()));
            } catch (Exception e) {
                model.addAttribute("error", "Upload failed");
                model.addAttribute("user", user);
                return "admin/user-creation";
            }

        }
//         Save hashed password
        user.setPassword(valid.hashPassword(user.getPassword()));
        userService.save(user);
        redirectAttributes.addFlashAttribute("success", "User created successfully");
        //redirect to user list page
        return "redirect:/admin/user";
    }

    @GetMapping("/update")
    public String showUpdateForm(HttpServletRequest request,
                                 Model model,
                                 RedirectAttributes redirectAttributes) {
        String sId = request.getParameter("id");
        if (sId == null || sId.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "User not found");
            return "redirect:/admin/user";
        }

        Long id = Long.parseLong(sId);
        Users user = userService.findById(id);

        if (user == null) {
            redirectAttributes.addFlashAttribute("error", "User not found");
            return "redirect:/admin/user";
        }

        model.addAttribute("user", user);
        return "admin/user-update";
    }

    @PostMapping("/update")
    public String updateUser(HttpServletRequest request,
                             RedirectAttributes redirectAttributes,
                             Model model) {
        String sId = request.getParameter("id");
        if (sId == null || sId.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "User not found");
            return "redirect:/admin/user";
        }

        Long id;
        try {
            id = Long.parseLong(sId.trim());
        } catch (NumberFormatException ex) {
            redirectAttributes.addFlashAttribute("error", "Invalid user ID");
            return "redirect:/admin/user";
        }

        Users user = userService.findById(id);
        if (user == null) {
            redirectAttributes.addFlashAttribute("error", "User not found");
            return "redirect:/admin/user";
        }

        // update fields (chỉ update những field được phép)
        try {
            String password = request.getParameter("password");
            if (password != null) user.setPassword(password);

            String userType = request.getParameter("userType");
            if (userType != null) user.setUserType(userType);

            String phone = request.getParameter("phoneNumber");
            if (phone != null) user.setPhoneNumber(phone);

            String avatar = request.getParameter("avatarUrl");
            if (avatar != null) user.setAvatarUrl(avatar);

            String gender = request.getParameter("gender");
            if (gender != null) user.setGender(gender);

            String birthDate = request.getParameter("birthDate");
            if (birthDate != null && !birthDate.isEmpty()) {
                try {
                    user.setBirthDate(LocalDate.parse(birthDate)); // expects yyyy-MM-dd
                } catch (DateTimeParseException dtpe) {
                    // trả về lại form với thông báo, giữ user hiện tại (không redirect)
                    model.addAttribute("error", "Invalid birth date format. Expected yyyy-MM-dd");
                    model.addAttribute("user", user);
                    return "admin/user-update";
                }
            }

            user.setUpdatedAt(LocalDateTime.now());

            // Lưu và lấy lại entity (nếu service trả về saved entity thì dùng kết quả)
            userService.save(user);
            // user = userService.findById(id); // tuỳ service, có thể reload để chắc chắn

            model.addAttribute("user", user);
            model.addAttribute("success", "Updated user successfully");
            return "admin/user-update";

        } catch (Exception e) {
            // Bắt mọi lỗi bất ngờ, trả về form với error message
            model.addAttribute("error", "An error occurred while updating user: " + e.getMessage());
            model.addAttribute("user", user);
            return "admin/user-update";
        }
    }

    @PostMapping("/delete")
    public String deleteUser(HttpServletRequest request, RedirectAttributes redirectAttributes) {
        String sId = request.getParameter("id");
        if (sId == null || sId.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "User not found");
        } else {
            Long id = Long.parseLong(sId);
            Users user = userService.findById(id);
            if (user == null) {
                redirectAttributes.addFlashAttribute("error", "User not found");
            } else {
                userService.delete(id);
                redirectAttributes.addFlashAttribute("success", "Removed user successfully");

            }
        }
        return "redirect:/admin/user";
    }

}
