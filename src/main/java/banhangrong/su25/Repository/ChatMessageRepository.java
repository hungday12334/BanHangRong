package banhangrong.su25.Repository;

import banhangrong.su25.Entity.ChatMessage;
import banhangrong.su25.Entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findByRoomOrderByCreatedAtAsc(ChatRoom room);

    // SỬA LỖI: Thay vì dùng method name, dùng @Query để so sánh sender.userId
    @Query("SELECT cm FROM ChatMessage cm WHERE cm.room = :room AND cm.isRead = false AND cm.sender.userId != :senderId")
    List<ChatMessage> findUnreadMessagesByRoomAndSenderNot(@Param("room") ChatRoom room, @Param("senderId") Long senderId);

    @Query("SELECT COUNT(cm) FROM ChatMessage cm WHERE cm.room = :room AND cm.isRead = false AND cm.sender.userId != :userId")
    Long countUnreadMessages(@Param("room") ChatRoom room, @Param("userId") Long userId);

    @Modifying
    @Query("UPDATE ChatMessage cm SET cm.isRead = true, cm.readAt = CURRENT_TIMESTAMP WHERE cm.room = :room AND cm.sender.userId != :userId AND cm.isRead = false")
    void markMessagesAsRead(@Param("room") ChatRoom room, @Param("userId") Long userId);
}