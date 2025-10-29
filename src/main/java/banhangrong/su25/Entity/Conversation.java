package banhangrong.su25.Entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "chat_conversations",
        indexes = {
                @Index(name = "idx_customer", columnList = "customer_id"),
                @Index(name = "idx_seller", columnList = "seller_id"),
                @Index(name = "idx_last_message_time", columnList = "last_message_time")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_customer_seller", columnNames = {"customer_id", "seller_id"})
        }
)
public class Conversation {
    @Id
    @Column(length = 100)
    private String id;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "customer_name", length = 100)
    private String customerName;

    @Column(name = "seller_id", nullable = false)
    private Long sellerId;

    @Column(name = "seller_name", length = 100)
    private String sellerName;

    @Column(name = "last_message", columnDefinition = "TEXT")
    private String lastMessage;

    @Column(name = "last_message_time")
    private LocalDateTime lastMessageTime;

    @Transient // Not persisted to database
    private int unreadCount = 0;

    @Transient // Not persisted, loaded separately
    private List<ChatMessage> messages = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}

