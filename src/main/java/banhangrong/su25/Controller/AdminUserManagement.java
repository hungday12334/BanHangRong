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
        user.setIsActive(true); // ⚠️ Account inactive until email verification
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
                    <div style="background-color: #fff3cd; border-left: 4px solid #ffc107; padding: 12px; border-radius: 6px; margin: 15px 0;">
                        <p style="margin: 0; color: #856404;"><strong>⚠️ Quan trọng:</strong> Tài khoản của bạn hiện đang ở trạng thái <strong>TẠM THỜI</strong>.</p>
                        <p style="margin: 8px 0 0; color: #856404;">Bạn cần <strong>xác minh email</strong> khi đăng nhập lần đầu để kích hoạt tài khoản chính thức.</p>
                    </div>
                    <p>Sau khi xác minh email thành công, tài khoản sẽ được chuyển sang trạng thái <strong>CHÍNH THỨC</strong> và bạn có thể sử dụng đầy đủ tính năng.</p>
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
            if(password !=null && !password.equals(user.getPassword())){
                user.setPassword(valid.hashPassword(password));
            }
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
            String subject = "BanHangRong - Change email address";
            if (!preEmail.equals(email)) {

                String oldMailMsg = """
                        <div style="font-family: Arial, sans-serif; color: #333; max-width: 600px; margin: auto; border: 1px solid #eee; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 12px rgba(0,0,0,0.05);">
                            <!-- Header cam cảnh báo -->
                            <div style="background: linear-gradient(135deg, #fd7e14, #f39c12); padding: 20px; text-align: center; color: white;">
                                <h2 style="margin: 0; font-size: 24px;">
                                    Email đã bị thay đổi
                                </h2>
                            </div>
                        
                            <!-- Nội dung -->
                            <div style="padding: 25px; background-color: #fff;">
                                <p style="font-size: 16px; line-height: 1.6;">
                                    Xin chào,
                                </p>
                                <p style="font-size: 16px; line-height: 1.6;">
                                    Email đăng ký của tài khoản <strong>%s</strong> trên hệ thống <strong>BanHangRong</strong> <span style="color: #e67e22; font-weight: bold;">đã bị thay đổi</span>.
                                </p>
                        
                                <div style="background-color: #fff8f0; border-left: 4px solid #fd7e14; padding: 15px; margin: 20px 0; font-size: 15px;">
                                    <p style="margin: 0;"><strong>Tài khoản:</strong> %s</p>
                                    <p style="margin: 8px 0 0;"><strong>Thời gian thay đổi:</strong> vừa xong</p>
                                </div>
                        
                                <p style="font-size: 16px; line-height: 1.6; color: #d35400;">
                                    <strong>Nếu bạn KHÔNG thực hiện thay đổi này</strong>, vui lòng liên hệ ngay với chúng tôi!
                                </p>
                        
                                <div style="text-align: center; margin: 25px 0;">
                                    <a href="mailto:bonhoangncd@gmail.com" 
                                       style="background-color: #fd7e14; color: white; padding: 12px 30px; text-decoration: none; border-radius: 8px; font-weight: bold; display: inline-block;">
                                       Liên hệ hỗ trợ ngay
                                    </a>
                                </div>
                            </div>
                        
                            <!-- Footer -->
                            <div style="background-color: #f8f9fa; padding: 15px; text-align: center; font-size: 13px; color: #777; border-top: 1px solid #eee;">
                                <p style="margin: 5px 0;">
                                    Trân trọng,<br>
                                    <strong>Đội ngũ BanHangRong</strong>
                                </p>
                            </div>
                        </div>
                        """.formatted(user.getUsername(), user.getUsername());
                if(preEmail!=null && !preEmail.isEmpty()){
                    emailService.sendEmail(new Email(preEmail, subject, oldMailMsg));
                }


                String newMailMsg = """
                        <div style="font-family: Arial, sans-serif; color: #333; max-width: 600px; margin: auto; border: 1px solid #eee; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 12px rgba(0,0,0,0.05);">
                            <!-- Header xanh lá -->
                            <div style="background: linear-gradient(135deg, #28a745, #20c997); padding: 20px; text-align: center; color: white;">
                                <h2 style="margin: 0; font-size: 24px;">
                                    Email đã được cập nhật
                                </h2>
                            </div>
                        
                            <!-- Nội dung -->
                            <div style="padding: 25px; background-color: #fff;">
                                <p style="font-size: 16px; line-height: 1.6;">
                                    Xin chào <strong>%s</strong>,
                                </p>
                                <p style="font-size: 16px; line-height: 1.6;">
                                    Tài khoản của bạn trên <strong>BanHangRong</strong> đã được liên kết thành công với <strong>địa chỉ email mới này</strong>.
                                </p>
                        
                                <div style="background-color: #f8fff9; border-left: 4px solid #28a745; padding: 15px; margin: 20px 0; font-size: 15px;">
                                    <p style="margin: 0;"><strong>Tài khoản:</strong> %s</p>
                                    <p style="margin: 8px 0 0;"><strong>Email mới:</strong> %s</p>
                                </div>
                        
                                <p style="font-size: 16px; line-height: 1.6;">
                                    Từ bây giờ, hãy sử dụng email này để <strong>đăng nhập</strong> và <strong>xác minh tài khoản</strong>.
                                </p>
                        
                                <div style="text-align: center; margin: 25px 0;">
                                    <a href="https://banhangrong.com/login" 
                                       style="background-color: #28a745; color: white; padding: 12px 30px; text-decoration: none; border-radius: 8px; font-weight: bold; display: inline-block;">
                                       Đăng nhập ngay
                                    </a>
                                </div>
                            </div>
                        
                            <!-- Footer -->
                            <div style="background-color: #f8f9fa; padding: 15px; text-align: center; font-size: 13px; color: #777; border-top: 1px solid #eee;">
                                <p style="margin: 5px 0;">
                                    Trân trọng,<br>
                                    <strong>Đội ngũ BanHangRong</strong>
                                </p>
                            </div>
                        </div>
                        """.formatted(user.getUsername(), user.getUsername(), email);
                if(email!=null && !email.isEmpty()){
                    emailService.sendEmail(new Email(email, subject, newMailMsg));
                }
                user.setIsEmailVerified(false);
                model.addAttribute("success", "User information updated successfully and email notifications sent.");
            } else {
                subject = "BanHangRong - Change User Information";
                String infoChangeMsg = """
                        <div style="font-family: Arial, sans-serif; color: #333; max-width: 600px; margin: auto; border: 1px solid #eee; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 12px rgba(0,0,0,0.05);">
                            <!-- Header xanh dương -->
                            <div style="background: linear-gradient(135deg, #2c7be5, #3498db); padding: 20px; text-align: center; color: white;">
                                <h2 style="margin: 0; font-size: 24px;">
                                    Thông tin tài khoản đã được cập nhật
                                </h2>
                            </div>
                        
                            <!-- Nội dung -->
                            <div style="padding: 25px; background-color: #fff;">
                                <p style="font-size: 16px; line-height: 1.6;">
                                    Xin chào <strong>%s</strong>,
                                </p>
                                <p style="font-size: 16px; line-height: 1.6;">
                                    Quản trị viên vừa <strong>cập nhật một số thông tin</strong> trong tài khoản của bạn trên hệ thống <strong>BanHangRong</strong>.
                                </p>
                        
                                <div style="background-color: #f0f8ff; border-left: 4px solid #2c7be5; padding: 15px; margin: 20px 0; font-size: 15px;">
                                    <p style="margin: 0;"><strong>Tài khoản:</strong> %s</p>
                                    <p style="margin: 8px 0 0;"><strong>Thời gian cập nhật:</strong> vừa xong</p>
                                </div>
                        
                                <p style="font-size: 16px; line-height: 1.6;">
                                    Nếu bạn <strong>không yêu cầu thay đổi</strong> hoặc cần hỗ trợ, vui lòng liên hệ ngay.
                                </p>
                        
                                <div style="text-align: center; margin: 25px 0;">
                                    <a href="mailto:bonhoangncd@gmail.com" 
                                       style="background-color: #2c7be5; color: white; padding: 12px 30px; text-decoration: none; border-radius: 8px; font-weight: bold; display: inline-block;">
                                       Liên hệ hỗ trợ
                                    </a>
                                </div>
                            </div>
                        
                            <!-- Footer -->
                            <div style="background-color: #f8f9fa; padding: 15px; text-align: center; font-size: 13px; color: #777; border-top: 1px solid #eee;">
                                <p style="margin: 5px 0;">
                                    Trân trọng,<br>
                                    <strong>Đội ngũ BanHangRong</strong>
                                </p>
                            </div>
                        </div>
                        """.formatted(user.getUsername(), user.getUsername());
                if(user.getEmail()!=null && !user.getEmail().isEmpty()){
                    emailService.sendEmail(new Email(user.getEmail(),subject,infoChangeMsg));
                }
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
                <div style="font-family: Arial, sans-serif; color: #333; max-width: 600px; margin: auto; border: 1px solid #eee; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 12px rgba(0,0,0,0.05);">
                    <!-- Header đỏ cảnh báo -->
                    <div style="background: linear-gradient(135deg, #dc3545, #e74c3c); padding: 20px; text-align: center; color: white;">
                        <h2 style="margin: 0; font-size: 24px;">
                            Tài khoản đã bị vô hiệu hóa
                        </h2>
                    </div>
                
                    <!-- Nội dung -->
                    <div style="padding: 25px; background-color: #fff;">
                        <p style="font-size: 16px; line-height: 1.6;">
                            Xin chào <strong>%s</strong>,
                        </p>
                        <p style="font-size: 16px; line-height: 1.6;">
                            Chúng tôi rất tiếc phải thông báo rằng tài khoản của bạn trên hệ thống <strong>BanHangRong</strong> đã bị <strong>vô hiệu hóa</strong> bởi quản trị viên.
                        </p>
                
                        <div style="background-color: #fff5f5; border-left: 4px solid #dc3545; padding: 15px; margin: 20px 0; font-size: 15px;">
                            <p style="margin: 0;"><strong>Tài khoản:</strong> %s</p>
                            <p style="margin: 8px 0 0;"><strong>Thời gian vô hiệu hóa:</strong> vừa xong</p>
                        </div>
                
                        <div style="background-color: #fdf2f2; border: 1px solid #f5c6cb; border-radius: 8px; padding: 15px; margin: 20px 0; font-size: 15px; color: #721c24;">
                            <p style="margin: 0; font-weight: bold;">Lý do:</p>
                            <p style="margin: 8px 0 0; font-style: italic;">"%s"</p>
                        </div>
                
                        <p style="font-size: 16px; line-height: 1.6; color: #721c24;">
                            <strong>Bạn không thể đăng nhập</strong> cho đến khi tài khoản được kích hoạt lại.
                        </p>
                
                        <p style="font-size: 16px; line-height: 1.6;">
                            Nếu bạn cho rằng đây là nhầm lẫn, vui lòng <strong>liên hệ ngay</strong> với chúng tôi để được hỗ trợ.
                        </p>
                
                        <div style="text-align: center; margin: 30px 0;">
                            <a href="mailto:bonhoangncd@gmail.com" 
                               style="background-color: #dc3545; color: white; padding: 12px 30px; text-decoration: none; border-radius: 8px; font-weight: bold; display: inline-block; box-shadow: 0 4px 8px rgba(220,53,69,0.3);">
                               Liên hệ hỗ trợ ngay
                            </a>
                        </div>
                    </div>
                
                    <!-- Footer -->
                    <div style="background-color: #f8f9fa; padding: 15px; text-align: center; font-size: 13px; color: #777; border-top: 1px solid #eee;">
                        <p style="margin: 5px 0;">
                            Trân trọng,<br>
                            <strong>Đội ngũ BanHangRong</strong>
                        </p>
                        <p style="margin: 8px 0 0; font-size: 12px;">
                            © 2025 BanHangRong. All rights reserved.
                        </p>
                    </div>
                </div>
                """.formatted(
                user.getUsername(),           // %s đầu tiên: username
                user.getUsername(),           // %s thứ hai: username (trong thông tin)
                reason                        // %s thứ ba: lý do
        );

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
        userService.activeUserById(user);
        redirectAttributes.addFlashAttribute("success", "Activated user successfully");

        String subject = "BanHangRong - Your Account Has Been Activated";
        String message = """
                <div style="font-family: Arial, sans-serif; color: #333; max-width: 600px; margin: auto; border: 1px solid #eee; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 12px rgba(0,0,0,0.05);">
                    <!-- Header xanh lá thành công -->
                    <div style="background: linear-gradient(135deg, #00A86B, #20c997); padding: 20px; text-align: center; color: white;">
                        <h2 style="margin: 0; font-size: 24px;">
                            Tài khoản đã được mở lại
                        </h2>
                    </div>
                
                    <!-- Nội dung -->
                    <div style="padding: 25px; background-color: #fff;">
                        <p style="font-size: 16px; line-height: 1.6;">
                            Xin chào <strong>%s</strong>,
                        </p>
                        <p style="font-size: 16px; line-height: 1.6;">
                            Chúng tôi rất vui mừng thông báo rằng tài khoản của bạn trên hệ thống <strong>BanHangRong</strong> đã được <strong>mở lại thành công</strong> bởi quản trị viên.
                        </p>
                
                        <div style="background-color: #f8fff9; border-left: 4px solid #00A86B; padding: 15px; margin: 20px 0; font-size: 15px;">
                            <p style="margin: 0;"><strong>Tài khoản:</strong> %s</p>
                            <p style="margin: 8px 0 0;"><strong>Thời gian mở lại:</strong> vừa xong</p>
                        </div>
                
                        <p style="font-size: 16px; line-height: 1.6;">
                            Bây giờ bạn <strong>có thể đăng nhập lại</strong> và sử dụng đầy đủ các tính năng như bình thường.
                        </p>
                
                        <div style="text-align: center; margin: 30px 0;">
                            <a href="https://banhangrong.com/login" 
                               style="background-color: #00A86B; color: white; padding: 12px 30px; text-decoration: none; border-radius: 8px; font-weight: bold; display: inline-block; box-shadow: 0 4px 8px rgba(0,168,107,0.3);">
                               Đăng nhập ngay
                            </a>
                        </div>
                
                        <p style="font-size: 14px; color: #666; line-height: 1.6;">
                            Nếu bạn cần hỗ trợ hoặc có thắc mắc, vui lòng liên hệ:
                            <a href="mailto:bonhoangncd@gmail.com" style="color: #00A86B; font-weight: bold;">bonhoangncd@gmail.com </a>
                        </p>
                    </div>
                
                    <!-- Footer -->
                    <div style="background-color: #f8f9fa; padding: 15px; text-align: center; font-size: 13px; color: #777; border-top: 1px solid #eee;">
                        <p style="margin: 5px 0;">
                            Trân trọng,<br>
                            <strong>Đội ngũ BanHangRong</strong>
                        </p>
                        <p style="margin: 8px 0 0; font-size: 12px;">
                            © 2025 BanHangRong. All rights reserved.
                        </p>
                    </div>
                </div>
                """.formatted(
                user.getUsername(),     // %s đầu tiên: chào
                user.getUsername()      // %s thứ hai: trong thông tin
        );

        try {
            emailService.sendEmail(new Email(user.getEmail(), subject, message));
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "An error occurred while sending email: " + e.getMessage());
        }

        return "redirect:/admin/user";
    }

}