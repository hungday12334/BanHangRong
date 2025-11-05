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
                    int successCount = 0;
                    for (Users customer : customers) {
                        try {
                            // Tạo notification chào mừng
                            Notification welcomeNotification = new Notification();
                            welcomeNotification.setUserId(customer.getUserId());
                            welcomeNotification.setTitle("Welcome to BanHangRong!");
                            welcomeNotification.setMessage("Thank you for registering. Explore our amazing products!");
                            welcomeNotification.setType("SYSTEM");
                            welcomeNotification.setIsRead(false);
                            notificationRepository.save(welcomeNotification);
                            successCount++;
                        } catch (Exception ex) {
                            System.err.println("⚠ Failed to create notification for user " + customer.getUserId() + ": " + ex.getMessage());
                            // Nếu lỗi là do encoding, thông báo cho admin
                            if (ex.getMessage() != null && ex.getMessage().contains("Incorrect string value")) {
                                System.err.println("⚠ DATABASE CHARSET ERROR: Please run sql/fix_notifications_charset.sql to fix UTF-8 encoding!");
                                break; // Dừng vòng lặp vì tất cả sẽ lỗi
                            }
                        }
                    }
                    
                    if (successCount > 0) {
                        System.out.println("✓ Created " + successCount + " welcome notifications");
                    } else {
                        System.out.println("⚠ Could not create notifications - please check database charset");
                    }
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

