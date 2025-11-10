package banhangrong.su25.Repository;

import banhangrong.su25.Entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findByConversationIdOrderByCreatedAtAsc(String conversationId);


    @Query("SELECT cm FROM ChatMessage cm WHERE cm.conversationId = :conversationId AND cm.read = false AND cm.senderId != :senderId")
    List<ChatMessage> findUnreadMessagesByConversationIdAndSenderNot(@Param("conversationId") String conversationId, @Param("senderId") Long senderId);

    @Query("SELECT COUNT(cm) FROM ChatMessage cm WHERE cm.conversationId = :conversationId AND cm.read = false AND cm.senderId != :userId")
    Long countUnreadMessages(@Param("conversationId") String conversationId, @Param("userId") Long userId);

    @Modifying
    @Query("UPDATE ChatMessage cm SET cm.read = true WHERE cm.conversationId = :conversationId AND cm.senderId != :userId AND cm.read = false")
    void markMessagesAsRead(@Param("conversationId") String conversationId, @Param("userId") Long userId);

    @Modifying
    @Query("UPDATE ChatMessage cm SET cm.read = true WHERE cm.conversationId = :conversationId AND cm.senderId != :userId")
    void markConversationAsRead(@Param("conversationId") String conversationId, @Param("userId") Long userId);

    @Query("SELECT COUNT(cm) FROM ChatMessage cm WHERE cm.conversationId = :conversationId")
    Long countMessagesByConversationId(@Param("conversationId") String conversationId);

    @Query("SELECT cm FROM ChatMessage cm WHERE cm.conversationId = :conversationId ORDER BY cm.createdAt ASC")
    List<ChatMessage> findMessagesByConversation(@Param("conversationId") String conversationId);
}