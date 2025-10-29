package banhangrong.su25.Repository;


import banhangrong.su25.Entity.ChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findByConversationIdOrderByCreatedAtAsc(String conversationId);

    Page<ChatMessage> findByConversationIdOrderByCreatedAtDesc(String conversationId, Pageable pageable);

    @Query("SELECT COUNT(m) FROM ChatMessage m WHERE m.conversationId = :conversationId AND m.receiverId = :receiverId AND m.read = false")
    long countUnreadMessages(@Param("conversationId") String conversationId, @Param("receiverId") Long receiverId);

    @Modifying
    @Query("UPDATE ChatMessage m SET m.read = true WHERE m.conversationId = :conversationId AND m.receiverId = :receiverId AND m.read = false")
    void markConversationAsRead(@Param("conversationId") String conversationId, @Param("receiverId") Long receiverId);

    List<ChatMessage> findTop50ByConversationIdOrderByCreatedAtDesc(String conversationId);
}

