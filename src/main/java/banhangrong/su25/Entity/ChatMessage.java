package banhangrong.su25.Entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "chat_messages")
public class ChatMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "message_id")
    private Long id;

    @Column(name = "conversation_id", nullable = false, length = 100)
    private String conversationId;

    // 🚨 SỬA: room_id là kiểu Long (BIGINT)
    @Column(name = "room_id", nullable = true) // Cho phép NULL
    private Long roomId;

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

    @Column(name = "message_type", length = 20)
    private String messageType = "TEXT";

    @Column(name = "is_read")
    private Boolean read = false;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "file_url")
    private String fileUrl;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    // Constructor
    public ChatMessage(String conversationId, Long senderId, Long receiverId, String content) {
        this.conversationId = conversationId;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.content = content;
        this.createdAt = LocalDateTime.now();
        this.messageType = "TEXT";
        this.read = false;
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (messageType == null) {
            messageType = "TEXT";
        }
        if (read == null) {
            read = false;
        }
        // 🚨 KHÔNG set room_id - để nó là NULL
    }

    public String getTimestamp() {
        return createdAt != null ? createdAt.toString() : LocalDateTime.now().toString();
    }
}