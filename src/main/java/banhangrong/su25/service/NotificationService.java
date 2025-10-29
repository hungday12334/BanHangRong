package banhangrong.su25.service;

import banhangrong.su25.Entity.Notification;
import banhangrong.su25.Repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;

    /**
     * Tạo thông báo mới
     */
    @Transactional
    public Notification createNotification(Long userId, String title, String message, String type, Long referenceId) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setType(type);
        notification.setReferenceId(referenceId);
        notification.setIsRead(false);
        
        return notificationRepository.save(notification);
    }

    /**
     * Tạo thông báo khi mua hàng thành công
     */
    @Transactional
    public void createOrderNotification(Long userId, Long orderId, String orderCode) {
        String title = "Đặt hàng thành công";
        String message = String.format("Đơn hàng #%s của bạn đã được đặt thành công. Vui lòng chờ người bán xác nhận.", orderCode);
        createNotification(userId, title, message, "ORDER", orderId);
    }

    /**
     * Tạo thông báo khi đánh giá thành công
     */
    @Transactional
    public void createReviewNotification(Long userId, Long reviewId, String productName) {
        String title = "Đánh giá thành công";
        String message = String.format("Cảm ơn bạn đã đánh giá sản phẩm \"%s\". Đánh giá của bạn đã được ghi nhận.", productName);
        createNotification(userId, title, message, "REVIEW", reviewId);
    }

    /**
     * Lấy danh sách thông báo với bộ lọc
     */
    public Page<Notification> getNotificationsWithFilters(
            Long userId, 
            String type, 
            Boolean isRead, 
            LocalDateTime startDate, 
            LocalDateTime endDate,
            String sortBy,
            int page, 
            int size) {
        
        Sort sort;
        if ("oldest".equals(sortBy)) {
            sort = Sort.by("createdAt").ascending();
        } else {
            sort = Sort.by("createdAt").descending(); // Mặc định mới nhất
        }
        
        Pageable pageable = PageRequest.of(page, size, sort);
        
        return notificationRepository.findWithFilters(userId, type, isRead, startDate, endDate, pageable);
    }

    /**
     * Lấy tất cả thông báo của user
     */
    public Page<Notification> getUserNotifications(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    /**
     * Đánh dấu đã đọc
     */
    @Transactional
    public boolean markAsRead(Long notificationId, Long userId) {
        Optional<Notification> optNotification = notificationRepository.findById(notificationId);
        
        if (optNotification.isPresent()) {
            Notification notification = optNotification.get();
            
            // Kiểm tra notification có thuộc về user không
            if (!notification.getUserId().equals(userId)) {
                return false;
            }
            
            notification.setIsRead(true);
            notification.setReadAt(LocalDateTime.now());
            notificationRepository.save(notification);
            return true;
        }
        
        return false;
    }

    /**
     * Đánh dấu tất cả đã đọc
     */
    @Transactional
    public void markAllAsRead(Long userId) {
        Page<Notification> unreadNotifications = notificationRepository.findByUserIdAndIsReadOrderByCreatedAtDesc(
            userId, false, PageRequest.of(0, 1000)
        );
        
        LocalDateTime now = LocalDateTime.now();
        unreadNotifications.forEach(notification -> {
            notification.setIsRead(true);
            notification.setReadAt(now);
        });
        
        notificationRepository.saveAll(unreadNotifications.getContent());
    }

    /**
     * Đếm số thông báo chưa đọc
     */
    public Long countUnreadNotifications(Long userId) {
        return notificationRepository.countByUserIdAndIsRead(userId, false);
    }

    /**
     * Lấy chi tiết notification
     */
    public Optional<Notification> getNotificationById(Long notificationId, Long userId) {
        Optional<Notification> notification = notificationRepository.findById(notificationId);
        
        // Kiểm tra notification có thuộc về user không
        if (notification.isPresent() && !notification.get().getUserId().equals(userId)) {
            return Optional.empty();
        }
        
        return notification;
    }

    /**
     * Xóa notification
     */
    @Transactional
    public boolean deleteNotification(Long notificationId, Long userId) {
        Optional<Notification> notification = notificationRepository.findById(notificationId);
        
        if (notification.isPresent() && notification.get().getUserId().equals(userId)) {
            notificationRepository.delete(notification.get());
            return true;
        }
        
        return false;
    }
}

