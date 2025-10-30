package banhangrong.su25.Controller;

import banhangrong.su25.DTO.UserFilter;
import banhangrong.su25.Entity.Products;
import banhangrong.su25.Entity.Users;
import banhangrong.su25.Util.ImageUploadUtil;
import banhangrong.su25.Util.Validation;
import banhangrong.su25.service.AdminProductService;
import banhangrong.su25.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/admin/user")
public class AdminUserManagement {

    @Autowired
    private UserService userService;

    @GetMapping("filter")
    public String filterUser(@ModelAttribute("filter") UserFilter userFilter, RedirectAttributes redirectAttributes) {

        List<Users> listFilterUser = userService.filter(userFilter);
        redirectAttributes.addFlashAttribute("filter", listFilterUser);
        redirectAttributes.addFlashAttribute("isFromFilter", true);
        return "redirect:/admin/user";
    }

    @GetMapping("/create")
    public String showCreateScreen(Model model) {
        return "admin/user-creation";
    }

    @PostMapping("/create")
    public String createUser(HttpServletRequest request, Model model, RedirectAttributes redirectAttributes) {
        //Delare
        Validation valid = new Validation();
        LocalDateTime now = LocalDateTime.now();
        Users user = new Users();
        MultipartHttpServletRequest multipartRequest = (MultipartHttpServletRequest) request;

        //Get infor from font-end and set to entity
        user.setUsername(request.getParameter("username"));
        user.setEmail(request.getParameter("email"));
        user.setPassword(request.getParameter("password"));
        user.setFullName(request.getParameter("fullName"));
        user.setUserType(request.getParameter("userType"));
        user.setPhoneNumber(request.getParameter("phoneNumber"));
        user.setAvatarUrl("");//Default null, if having image --> solving below
        user.setGender(request.getParameter("gender"));
        String birthDate = request.getParameter("birthDate");
        if (birthDate != null && !birthDate.isEmpty()) {
            user.setBirthDate(LocalDate.parse(birthDate));//from yyyy-MM-dd to LocalDate
        }
        //Check valid username and email
        if (userService.existsByUsername(user.getUsername()) || userService.existsByEmail(user.getEmail())) {
            model.addAttribute("error", "Username or Email already exists");
            model.addAttribute("user", user);
            return "admin/user-creation";
        } else {
            if (valid.hasSpace(user.getUsername())){
                model.addAttribute("error", "Username can not have space");
                model.addAttribute("user", user);
                return "admin/user-creation";
            }
            if(valid.hasSpace(user.getEmail())){
                model.addAttribute("error", "Email can not have space");
                model.addAttribute("user", user);
            }
        }

        //Check valid pasword
        if (!valid.isPasswordValid(user.getPassword())) {
            model.addAttribute("error", "Password must be at least 6 characters long");
            model.addAttribute("user", user);
            return "admin/user-creation";
        }else if(valid.hasSpace(user.getPassword())){
            model.addAttribute("error", "Password can not have space");
            model.addAttribute("user", user);
            return "admin/user-creation";
        }

        //Trim username
        if(user.getUsername()!=null){
            user.setUsername(user.getUsername().trim());
        }
        //Check valid phone
        if (user.getPhoneNumber() != null && !user.getPhoneNumber().isEmpty()) {
            if (!valid.isPhoneValid(user.getPhoneNumber())) {
                model.addAttribute("error", "Invalid phone number");
                model.addAttribute("user", user);
                return "admin/user-creation";
            }else if(valid.hasSpace(user.getPhoneNumber())){
                model.addAttribute("error", "Phone can not have space");
                model.addAttribute("user", user);
                return "admin/user-creation";
            }
        }
        user.setAvatarUrl(request.getParameter("imageUrl"));
//         Save hashed password
        user.setPassword(valid.hashPassword(user.getPassword()));
        //Blance default 0
        user.setBalance(BigDecimal.ZERO);
        user.setIsEmailVerified(true);
        user.setIsActive(true);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
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
        //Check id valid  start
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
        //Check id valid  end

        // Update fields (chỉ update những field được phép)
        try {
            //Delare
            ImageUploadUtil imageUploadUtil = new ImageUploadUtil();
            Validation valid = new Validation();
            LocalDateTime now = LocalDateTime.now();
            MultipartHttpServletRequest multipartRequest = (MultipartHttpServletRequest) request;

            //Get infor from font-end
            String email = request.getParameter("email");
            String password = request.getParameter("password");
            String fullName = request.getParameter("fullName");
            String phoneNumber = request.getParameter("phoneNumber");
            String gender = request.getParameter("gender");
            String birthDate = request.getParameter("birthDate");
            String balance = request.getParameter("balance");

            //Check valid email
            if(valid.hasSpace(email)){
                model.addAttribute("error", "Email can not have space");
                model.addAttribute("user", user);
                return "admin/user-update";
            }else if (userService.existsByEmail(email) && !email.equals(user.getEmail())) {
                //Kiem tra xem email da ton tai hay chua (Khong xet den email cu)
                model.addAttribute("error", "Email already exists");
                model.addAttribute("user", user);
                return "admin/user-update";
            }

            //Check valid pasword
            if (!valid.isPasswordValid(password)) {
                model.addAttribute("error", "Password must be at least 6 characters long");
                model.addAttribute("user", user);
                return "admin/user-update";
            }else if(valid.hasSpace(password)){
                model.addAttribute("error", "Password can not have space");
                model.addAttribute("user", user);
                return "admin/user-update";
            }
            //Trim full name
            if(fullName!=null){
                fullName=fullName.trim();
            }
            //Check valid phone
            if (user.getPhoneNumber() != null && !user.getPhoneNumber().isEmpty()) {
                if (!valid.isPhoneValid(phoneNumber)) {
                    model.addAttribute("error", "Invalid phone number");
                    model.addAttribute("user", user);
                    return "admin/user-update";
                }else if(valid.hasSpace(phoneNumber)){
                    model.addAttribute("error", "Phone can not have space");
                    model.addAttribute("user", user);
                    return "admin/user-update";
                }
            }
            //Check valid balance
            BigDecimal balanceD = null;
            if (balance != null && !balance.isEmpty()) {
                try {
                    balanceD = new BigDecimal(balance);
                    if (balanceD.compareTo(BigDecimal.ZERO) < 0) {
                        model.addAttribute("error", "Please enter a positive number");
                        model.addAttribute("user", user);
                        return "admin/user-update";
                    }
                    if (balanceD.compareTo(user.getBalance()) > 0) {
                    }
                } catch (Exception e) {
                    model.addAttribute("error", "Invalid balance");
                    model.addAttribute("user", user);
                    return "admin/user-update";
                }
            } else {
                balanceD = BigDecimal.ZERO;
            }
            // Solving if having image
            MultipartFile avatar = multipartRequest.getFile("avatarUrl");

            if (avatar != null && !avatar.isEmpty()) {

                // User entered an invalid file
                if (!valid.isImageFileValid(avatar)) {
                    model.addAttribute("error", "This file is not an image");
                    model.addAttribute("user", user);
                    return "admin/user-update";
                }

                // Save the image into static/img/avatar folder, naming follows role+id
                try {
                    user.setAvatarUrl(imageUploadUtil.saveAvatar(avatar, user.getUsername()));
                } catch (Exception e) {
                    model.addAttribute("error", "Upload failed");
                    model.addAttribute("user", user);
                    return "admin/user-update";
                }
            }
            user.setEmail(email);
//          Save hashed password
            user.setPassword(valid.hashPassword(password));
            user.setFullName(fullName);
            user.setPhoneNumber(phoneNumber);
            user.setGender(gender);
            if (!birthDate.isEmpty()) {
                user.setBirthDate(LocalDate.parse(birthDate));//from yyyy-MM-dd to LocalDate
            }
            user.setBalance(balanceD);
            user.setUpdatedAt(LocalDateTime.now());
            userService.save(user);
            model.addAttribute("success", "User created successfully");
            model.addAttribute("user", user);
            return "admin/user-update";

        } catch (Exception e) {
            // Bắt mọi lỗi bất ngờ, trả về form với error message
            model.addAttribute("error", "An error occurred while updating user: " + e.getMessage());
            model.addAttribute("user", user);
            return "admin/user-update";
        }
    }

    @PostMapping("/deactive")
    public String deactiveUser(HttpServletRequest request, RedirectAttributes redirectAttributes) {
        String sId = request.getParameter("id");
        if (sId == null || sId.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "User not found");
        } else {
            Long id = Long.parseLong(sId);
            Users user = userService.findById(id);
            if (user == null) {
                redirectAttributes.addFlashAttribute("error", "User not found");
            } else {
                userService.deactiveUserById(user);
                redirectAttributes.addFlashAttribute("success", "Deactivated user successfully");

            }
        }
        return "redirect:/admin/user";
    }

}
