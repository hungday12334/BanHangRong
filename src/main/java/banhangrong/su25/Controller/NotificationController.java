package banhangrong.su25.Controller;

import banhangrong.su25.Entity.Notification;
import banhangrong.su25.Entity.Users;
import banhangrong.su25.Repository.UsersRepository;
import banhangrong.su25.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/customer/notifications")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private UsersRepository usersRepository;

    /**
     * Trang danh sách thông báo
     */
    @GetMapping
    public String notificationsPage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Boolean isRead,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(defaultValue = "newest") String sortBy,
            Model model) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return "redirect:/login";
        }

        Users currentUser = usersRepository.findByUsername(auth.getName()).orElse(null);
        if (currentUser == null) {
            return "redirect:/login";
        }

        // Lấy danh sách notification với bộ lọc
        Page<Notification> notifications = notificationService.getNotificationsWithFilters(
            currentUser.getUserId(),
            type,
            isRead,
            startDate,
            endDate,
            sortBy,
            page,
            size
        );

        // Đếm số notification chưa đọc
        Long unreadCount = notificationService.countUnreadNotifications(currentUser.getUserId());

        model.addAttribute("notifications", notifications);
        model.addAttribute("unreadCount", unreadCount);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", notifications.getTotalPages());
        model.addAttribute("currentUser", currentUser);
        
        // Bộ lọc hiện tại
        model.addAttribute("currentType", type);
        model.addAttribute("currentIsRead", isRead);
        model.addAttribute("currentStartDate", startDate);
        model.addAttribute("currentEndDate", endDate);
        model.addAttribute("currentSortBy", sortBy);

        return "customer/notifications";
    }

    /**
     * API: Đánh dấu đã đọc
     */
    @PostMapping("/{notificationId}/mark-read")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> markAsRead(@PathVariable Long notificationId) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "Unauthorized"));
        }

        Users currentUser = usersRepository.findByUsername(auth.getName()).orElse(null);
        if (currentUser == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "Unauthorized"));
        }

        boolean success = notificationService.markAsRead(notificationId, currentUser.getUserId());
        Long unreadCount = notificationService.countUnreadNotifications(currentUser.getUserId());

        Map<String, Object> response = new HashMap<>();
        response.put("success", success);
        response.put("unreadCount", unreadCount);

        return ResponseEntity.ok(response);
    }

    /**
     * API: Đánh dấu tất cả đã đọc
     */
    @PostMapping("/mark-all-read")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> markAllAsRead() {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "Unauthorized"));
        }

        Users currentUser = usersRepository.findByUsername(auth.getName()).orElse(null);
        if (currentUser == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "Unauthorized"));
        }

        notificationService.markAllAsRead(currentUser.getUserId());
        Long unreadCount = notificationService.countUnreadNotifications(currentUser.getUserId());

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("unreadCount", unreadCount);

        return ResponseEntity.ok(response);
    }

    /**
     * API: Xóa notification
     */
    @DeleteMapping("/{notificationId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> deleteNotification(@PathVariable Long notificationId) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "Unauthorized"));
        }

        Users currentUser = usersRepository.findByUsername(auth.getName()).orElse(null);
        if (currentUser == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "Unauthorized"));
        }

        boolean success = notificationService.deleteNotification(notificationId, currentUser.getUserId());

        Map<String, Object> response = new HashMap<>();
        response.put("success", success);

        return ResponseEntity.ok(response);
    }

    /**
     * API: Đếm số notification chưa đọc (dùng cho badge)
     */
    @GetMapping("/unread-count")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getUnreadCount() {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "Unauthorized"));
        }

        Users currentUser = usersRepository.findByUsername(auth.getName()).orElse(null);
        if (currentUser == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "Unauthorized"));
        }

        Long unreadCount = notificationService.countUnreadNotifications(currentUser.getUserId());

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("count", unreadCount);

        return ResponseEntity.ok(response);
    }
}

