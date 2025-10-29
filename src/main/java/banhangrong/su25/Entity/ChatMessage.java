package banhangrong.su25.Entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "chat_messages",
        indexes = {
                @Index(name = "idx_conversation", columnList = "conversation_id"),
                @Index(name = "idx_sender", columnList = "sender_id"),
                @Index(name = "idx_receiver", columnList = "receiver_id"),
                @Index(name = "idx_created_at", columnList = "created_at"),
                @Index(name = "idx_is_read", columnList = "is_read")
        }
)
public class ChatMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "message_id")
    private Long id;

    @Column(name = "conversation_id", nullable = false, length = 100)
    private String conversationId;

    @Column(name = "sender_id", nullable = false)
    private Long senderId;

    @Column(name = "sender_name", length = 100)
    private String senderName;

    @Column(name = "sender_role", length = 20)
    private String senderRole;

    @Column(name = "receiver_id", nullable = false)
    private Long receiverId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Transient // For API compatibility, mapped to createdAt
    private String timestamp;

    @Column(name = "message_type", length = 20)
    private String type = "TEXT";

    @Column(name = "is_read", nullable = false)
    private boolean read = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;


    // Helper method to get timestamp as string for API
    public String getTimestamp() {
        return createdAt != null ? createdAt.toString() : timestamp;
    }

    // Helper method to set timestamp from string
    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
        // CreatedAt will be set by @CreationTimestamp
    }
}
