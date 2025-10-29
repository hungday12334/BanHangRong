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
     * Create new notification
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
     * Create notification when order is placed successfully
     */
    @Transactional
    public void createOrderNotification(Long userId, Long orderId, String orderCode) {
        String title = "Order Placed Successfully";
        String message = String.format("Your order #%s has been placed successfully. Please wait for seller confirmation.", orderCode);
        createNotification(userId, title, message, "ORDER", orderId);
    }

    /**
     * Create notification when review is submitted successfully
     */
    @Transactional
    public void createReviewNotification(Long userId, Long reviewId, String productName) {
        String title = "Review Submitted Successfully";
        String message = String.format("Thank you for reviewing product \"%s\". Your review has been recorded.", productName);
        createNotification(userId, title, message, "REVIEW", reviewId);
    }

    /**
     * Get notifications list with filters
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
            sort = Sort.by("createdAt").descending(); // Default: newest first
        }
        
        Pageable pageable = PageRequest.of(page, size, sort);
        
        return notificationRepository.findWithFilters(userId, type, isRead, startDate, endDate, pageable);
    }

    /**
     * Get all notifications for user
     */
    public Page<Notification> getUserNotifications(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    /**
     * Mark notification as read
     */
    @Transactional
    public boolean markAsRead(Long notificationId, Long userId) {
        Optional<Notification> optNotification = notificationRepository.findById(notificationId);
        
        if (optNotification.isPresent()) {
            Notification notification = optNotification.get();
            
            // Check if notification belongs to user
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
     * Mark all notifications as read
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
     * Count unread notifications
     */
    public Long countUnreadNotifications(Long userId) {
        return notificationRepository.countByUserIdAndIsRead(userId, false);
    }

    /**
     * Get notification details
     */
    public Optional<Notification> getNotificationById(Long notificationId, Long userId) {
        Optional<Notification> notification = notificationRepository.findById(notificationId);
        
        // Check if notification belongs to user
        if (notification.isPresent() && !notification.get().getUserId().equals(userId)) {
            return Optional.empty();
        }
        
        return notification;
    }

    /**
     * Delete notification
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

