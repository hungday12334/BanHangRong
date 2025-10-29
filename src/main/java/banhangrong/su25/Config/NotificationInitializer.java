package banhangrong.su25.Config;

import banhangrong.su25.Entity.Notification;
import banhangrong.su25.Entity.Users;
import banhangrong.su25.Repository.NotificationRepository;
import banhangrong.su25.Repository.UsersRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class NotificationInitializer implements CommandLineRunner {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private UsersRepository usersRepository;

    @Override
    public void run(String... args) throws Exception {
        try {
            System.out.println("=== Initializing Notifications System ===");
            
            // Kiểm tra xem đã có notification nào chưa
            long notificationCount = notificationRepository.count();
            
            if (notificationCount == 0) {
                System.out.println("Creating sample notifications for customers...");
                
                // Lấy tất cả customer
                List<Users> customers = usersRepository.findAll().stream()
                    .filter(u -> "CUSTOMER".equals(u.getUserType()))
                    .limit(10)
                    .toList();
                
                if (!customers.isEmpty()) {
                    for (Users customer : customers) {
                        // Tạo notification chào mừng
                        Notification welcomeNotification = new Notification();
                        welcomeNotification.setUserId(customer.getUserId());
                        welcomeNotification.setTitle("Chào mừng bạn đến với BanHangRong!");
                        welcomeNotification.setMessage("Cảm ơn bạn đã đăng ký tài khoản. Hãy khám phá các sản phẩm tuyệt vời của chúng tôi!");
                        welcomeNotification.setType("SYSTEM");
                        welcomeNotification.setIsRead(false);
                        notificationRepository.save(welcomeNotification);
                    }
                    
                    System.out.println("✓ Created " + customers.size() + " welcome notifications");
                } else {
                    System.out.println("⚠ No customers found to create sample notifications");
                }
            } else {
                System.out.println("✓ Notifications table already has data (" + notificationCount + " records)");
            }
            
            System.out.println("=== Notifications System Ready ===");
            
        } catch (Exception e) {
            System.err.println("⚠ Error initializing notifications: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

