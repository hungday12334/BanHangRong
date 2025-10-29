package banhangrong.su25.Repository;

import banhangrong.su25.Entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    
    // Lấy tất cả thông báo của user
    Page<Notification> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    
    // Lấy thông báo chưa đọc
    Page<Notification> findByUserIdAndIsReadOrderByCreatedAtDesc(Long userId, Boolean isRead, Pageable pageable);
    
    // Lấy thông báo theo loại
    Page<Notification> findByUserIdAndTypeOrderByCreatedAtDesc(Long userId, String type, Pageable pageable);
    
    // Đếm số thông báo chưa đọc
    Long countByUserIdAndIsRead(Long userId, Boolean isRead);
    
    // Lấy thông báo trong khoảng thời gian
    @Query("SELECT n FROM Notification n WHERE n.userId = :userId " +
           "AND n.createdAt >= :startDate AND n.createdAt <= :endDate " +
           "ORDER BY n.createdAt DESC")
    Page<Notification> findByUserIdAndDateRange(
        @Param("userId") Long userId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate,
        Pageable pageable
    );
    
    // Lấy danh sách thông báo theo các bộ lọc
    @Query("SELECT n FROM Notification n WHERE n.userId = :userId " +
           "AND (:type IS NULL OR n.type = :type) " +
           "AND (:isRead IS NULL OR n.isRead = :isRead) " +
           "AND (:startDate IS NULL OR n.createdAt >= :startDate) " +
           "AND (:endDate IS NULL OR n.createdAt <= :endDate)")
    Page<Notification> findWithFilters(
        @Param("userId") Long userId,
        @Param("type") String type,
        @Param("isRead") Boolean isRead,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate,
        Pageable pageable
    );
}

