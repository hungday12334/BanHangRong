package banhangrong.su25.DTO;

/**
 * DTO for emoji reaction on a message
 */
public class MessageReactionDTO {
    private String messageId;
    private String userId;
    private String userName;
    private String emoji;
    private String conversationId;
    private String action; // "add" or "remove"

    public MessageReactionDTO() {
    }

    public MessageReactionDTO(String messageId, String userId, String userName, String emoji, String conversationId, String action) {
        this.messageId = messageId;
        this.userId = userId;
        this.userName = userName;
        this.emoji = emoji;
        this.conversationId = conversationId;
        this.action = action;
    }

    // Getters and Setters
    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getEmoji() {
        return emoji;
    }

    public void setEmoji(String emoji) {
        this.emoji = emoji;
    }

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    @Override
    public String toString() {
        return "MessageReactionDTO{" +
                "messageId='" + messageId + '\'' +
                ", userId='" + userId + '\'' +
                ", userName='" + userName + '\'' +
                ", emoji='" + emoji + '\'' +
                ", conversationId='" + conversationId + '\'' +
                ", action='" + action + '\'' +
                '}';
    }
}

