package banhangrong.su25.Controller;

import banhangrong.su25.DTO.UserFilter;
import banhangrong.su25.Entity.Products;
import banhangrong.su25.Entity.Users;
import banhangrong.su25.Util.ImageUploadUtil;
import banhangrong.su25.Util.Validation;
import banhangrong.su25.email.Email;
import banhangrong.su25.email.EmailService;
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
    @Autowired
    private EmailService emailService;


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
            if (valid.hasSpace(user.getUsername())) {
                model.addAttribute("error", "Username can not have space");
                model.addAttribute("user", user);
                return "admin/user-creation";
            }
            if (valid.hasSpace(user.getEmail())) {
                model.addAttribute("error", "Email can not have space");
                model.addAttribute("user", user);
            }
        }

        //Check valid pasword
        if (!valid.isPasswordValid(user.getPassword())) {
            model.addAttribute("error", "Password must be at least 6 characters long");
            model.addAttribute("user", user);
            return "admin/user-creation";
        } else if (valid.hasSpace(user.getPassword())) {
            model.addAttribute("error", "Password can not have space");
            model.addAttribute("user", user);
            return "admin/user-creation";
        }

        //Trim username
        if (user.getUsername() != null) {
            user.setUsername(user.getUsername().trim());
        }
        //Check valid phone
        if (user.getPhoneNumber() != null && !user.getPhoneNumber().isEmpty()) {
            if (!valid.isPhoneValid(user.getPhoneNumber())) {
                model.addAttribute("error", "Invalid phone number");
                model.addAttribute("user", user);
                return "admin/user-creation";
            } else if (valid.hasSpace(user.getPhoneNumber())) {
                model.addAttribute("error", "Phone can not have space");
                model.addAttribute("user", user);
                return "admin/user-creation";
            }
        }
        user.setAvatarUrl(request.getParameter("imageUrl"));
//         Save hashed password
        String prePassword = user.getPassword();
        user.setPassword(valid.hashPassword(user.getPassword()));
        //Blance default 0
        user.setBalance(BigDecimal.ZERO);
        user.setIsEmailVerified(false);
        user.setIsActive(true);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userService.save(user);

        String subject = "Welcome to BanHangRong - Notification";

        String message = """
                <div style="font-family: Arial, sans-serif; color: #333;">
                    <h2 style="color: #2c7be5;">🎉 Chào mừng bạn đến với BanHangRong!</h2>
                    <p>Xin chào <strong>%s</strong>,</p>
                    <p>Admin vừa đăng ký thành công tài khoản của bạn trên hệ thống <strong>BanHangRong</strong>.</p>
                    <p>Dưới đây là thông tin đăng nhập:</p>
                    <div style="background-color: #f8f9fa; padding: 12px; border-radius: 6px; border: 1px solid #ddd; width: fit-content;">
                        <p><b>Email:</b> %s</p>
                        <p><b>Tài khoản:</b> %s</p>
                        <p><b>Mật khẩu:</b> %s</p>
                    </div>
                    <br>
                    <p>Khi đăng nhập lần đầu, bạn sẽ cần <strong>Verify email</strong> để xác minh tài khoản.</p>
                    <p>Hãy nhớ <strong>đổi mật khẩu</strong> sau khi đăng nhập để đảm bảo an toàn thông tin cá nhân.</p>
                    <hr>
                    <p style="font-size: 13px; color: #777;">Trân trọng,<br><em>Đội ngũ BanHangRong</em></p>
                </div>
                """.formatted(user.getEmail(), user.getEmail(), user.getUsername(), prePassword);

        emailService.sendEmail(
                new Email(user.getEmail(), subject, message)
        );

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
            String userType = request.getParameter("userType");
            String phoneNumber = request.getParameter("phoneNumber");
            String gender = request.getParameter("gender");
            String birthDate = request.getParameter("birthDate");
            String balance = request.getParameter("balance");

            //Check valid email
            if (valid.hasSpace(email)) {
                model.addAttribute("error", "Email can not have space");
                model.addAttribute("user", user);
                return "admin/user-update";
            } else if (userService.existsByEmail(email) && !email.equals(user.getEmail())) {
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
            } else if (valid.hasSpace(password)) {
                model.addAttribute("error", "Password can not have space");
                model.addAttribute("user", user);
                return "admin/user-update";
            }
            //Trim full name

            if (fullName != null) {
                fullName = fullName.trim();
            }
            //Check valid phone
            if (user.getPhoneNumber() != null && !user.getPhoneNumber().isEmpty()) {
                if (!valid.isPhoneValid(phoneNumber)) {
                    model.addAttribute("error", "Invalid phone number");
                    model.addAttribute("user", user);
                    return "admin/user-update";
                } else if (valid.hasSpace(phoneNumber)) {
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
            if (!user.getIsActive()) {
                model.addAttribute("error", "User is not active");
                model.addAttribute("user", user);
                return "admin/user-update";
            }
            String preEmail = user.getEmail();
            user.setEmail(email);
//          Save hashed password
            user.setPassword(valid.hashPassword(password));
            user.setAvatarUrl(request.getParameter("imageUrl"));
            user.setFullName(fullName);
            user.setUserType(userType);
            user.setPhoneNumber(phoneNumber);
            user.setGender(gender);
            if (!birthDate.isEmpty()) {
                user.setBirthDate(LocalDate.parse(birthDate));//from yyyy-MM-dd to LocalDate
            }
            user.setBalance(balanceD);
            user.setUpdatedAt(LocalDateTime.now());

            if (!preEmail.equals(email)) {
                String oldMailMsg = """
                            <div style="font-family: Arial,sans-serif; color:#333;">
                                <h3>🔔 Thông báo thay đổi email</h3>
                                <p>Xin chào,</p>
                                <p>Email đăng ký của tài khoản <b>%s</b> trong hệ thống <b>BanHangRong</b> vừa được thay đổi.</p>
            <p>Nếu bạn cho rằng đây là sự nhầm lẫn hoặc cần được hỗ trợ, vui lòng liên hệ với bộ phận hỗ trợ của chúng tôi qua email:
                <a href="mailto:bonhoangncd@gmail.com">bonhoangncd@gmail.com</a>.
            </p>
                                <hr>
                                <p style="font-size:13px;color:#777;">Trân trọng,<br>Đội ngũ BanHangRong</p>
                            </div>
                        """.formatted(user.getUsername());
                emailService.sendEmail(new Email(preEmail, "BanHangRong - Email Change Notification", oldMailMsg));
                String newMailMsg = """
                            <div style="font-family: Arial,sans-serif; color:#333;">
                                <h3>✅ Cập nhật email thành công</h3>
                                <p>Xin chào <b>%s</b>,</p>
                                <p>Tài khoản của bạn trên <b>BanHangRong</b> vừa được liên kết với địa chỉ email mới này.</p>
                                <p>Hãy dùng email này để đăng nhập và xác minh trong những lần tiếp theo.</p>
                                <hr>
                                <p style="font-size:13px;color:#777;">Trân trọng,<br>Đội ngũ BanHangRong</p>
                            </div>
                        """.formatted(user.getUsername());
                emailService.sendEmail(new Email(email, "BanHangRong - Email Updated Successfully", newMailMsg));
                user.setIsEmailVerified(false);
                model.addAttribute("success", "User information updated successfully and email notifications sent.");
            } else {
                String infoChangeMsg = """
                                        <div style="font-family: Arial,sans-serif; color:#333;">
                                            <h3 style="color:#2c7be5;">ℹ️ Thông tin tài khoản của bạn đã được cập nhật</h3>
                                            <p>Xin chào <b>%s</b>,</p>
                                            <p>Admin đã cập nhật một số thông tin trong tài khoản của bạn trên hệ thống <b>BanHangRong</b>.</p>
                           <p>Nếu bạn cho rằng đây là sự nhầm lẫn hoặc cần được hỗ trợ, vui lòng liên hệ với bộ phận hỗ trợ của chúng tôi qua email:
                            <a href="mailto:bonhoangncd@gmail.com">bonhoangncd@gmail.com</a>.
                        </p>
                                            <hr>
                                            <p style="font-size:13px;color:#777;">Trân trọng,<br>Đội ngũ BanHangRong</p>
                                        </div>
                        """.formatted(user.getUsername());
                emailService.sendEmail(new Email(email, "BanHangRong - Account Information Updated", infoChangeMsg));
                model.addAttribute("success", "User information updated successfully and notification sent.");
            }
            userService.save(user);
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
            return "redirect:/admin/user";
        }

        Long id = Long.parseLong(sId);
        Users user = userService.findById(id);

        if (user == null) {
            redirectAttributes.addFlashAttribute("error", "User not found");
            return "redirect:/admin/user";
        }
        if (!user.getIsActive()) {
            redirectAttributes.addFlashAttribute("error", "User is already deactivated");
            return "redirect:/admin/user";
        }
        String reason = request.getParameter("reason");
        userService.deactiveUserById(user);
        redirectAttributes.addFlashAttribute("success", "Deactivated user successfully");
        String subject = "BanHangRong - Your Account Has Been Deactivated";
        String message = """
                    <div style="font-family: Arial,sans-serif; color:#333;">
                        <h3 style="color:#d9534f;">⚠️ Tài khoản của bạn đã bị vô hiệu hóa</h3>
                        <p>Xin chào <b>%s</b>,</p>
                        <p>Tài khoản của bạn trên hệ thống <b>BanHangRong</b> đã bị <b>vô hiệu hóa (deactivated)</b> bởi quản trị viên. Tại vì: </p>
                        <p>%s</p>
                        <p>Nếu bạn cho rằng đây là sự nhầm lẫn hoặc cần được hỗ trợ, vui lòng liên hệ với bộ phận hỗ trợ của chúng tôi qua email:
                            <a href="mailto:bonhoangncd@gmail.com">bonhoangncd@gmail.com</a>.
                        </p>
                        <p>Bạn sẽ không thể đăng nhập cho đến khi tài khoản được kích hoạt lại.</p>
                        <hr>
                        <p style="font-size:13px;color:#777;">Trân trọng,<br>Đội ngũ <b>BanHangRong</b></p>
                    </div>
                """.formatted(user.getUsername(),reason);

        try {
            emailService.sendEmail(new Email(user.getEmail(), subject, message));
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "An error occurred while sending email: " + e.getMessage());
        }

        return "redirect:/admin/user";
    }
    @PostMapping("/active")
    public String activeUser(HttpServletRequest request, RedirectAttributes redirectAttributes) {
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
        if (user.getIsActive()) {
            redirectAttributes.addFlashAttribute("error", "User is already activated");
            return "redirect:/admin/user";
        }
        user.setIsActive(true);
        userService.save(user) ;
        redirectAttributes.addFlashAttribute("success", "Activated user successfully");

        String subject = "BanHangRong - Your Account Has Been Activated";
        String message = """
                    <div style="font-family: Arial,sans-serif; color:#333;">
                        <h3 style="color:#00A86B;"> Tài khoản của bạn đã được mở lại.</h3>
                        <p>Xin chào <b>%s</b>,</p>
                        <p>Tài khoản của bạn trên hệ thống <b>BanHangRong</b> đã được <b>mở lại </b> bởi quản trị viên.</p>
                        <p>Nếu bạn cho rằng đây là sự nhầm lẫn hoặc cần được hỗ trợ, vui lòng liên hệ với bộ phận hỗ trợ của chúng tôi qua email:
                            <a href="mailto:bonhoangncd@gmail.com">bonhoangncd@gmail.com</a>.
                        </p>
                        <p>Bây giờ bạn có thể đăng nhập lại và sử dụng như bình thường.</p>
                        <hr>
                        <p style="font-size:13px;color:#777;">Trân trọng,<br>Đội ngũ <b>BanHangRong</b></p>
                    </div>
                """.formatted(user.getUsername());

        try {
            emailService.sendEmail(new Email(user.getEmail(), subject, message));
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "An error occurred while sending email: " + e.getMessage());
        }

        return "redirect:/admin/user";
    }

}
