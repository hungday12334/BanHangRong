package banhangrong.su25.DTO;

/**
 * DTO for message deletion
 */
public class MessageDeleteDTO {
    private String messageId;
    private String conversationId;
    private String userId;
    private Boolean permanent; // true = permanent delete, false = soft delete

    public MessageDeleteDTO() {
    }

    public MessageDeleteDTO(String messageId, String conversationId, String userId, Boolean permanent) {
        this.messageId = messageId;
        this.conversationId = conversationId;
        this.userId = userId;
        this.permanent = permanent;
    }

    // Getters and Setters
    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Boolean getPermanent() {
        return permanent;
    }

    public void setPermanent(Boolean permanent) {
        this.permanent = permanent;
    }

    @Override
    public String toString() {
        return "MessageDeleteDTO{" +
                "messageId='" + messageId + '\'' +
                ", conversationId='" + conversationId + '\'' +
                ", userId='" + userId + '\'' +
                ", permanent=" + permanent +
                '}';
    }
}

