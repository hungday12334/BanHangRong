import java.io.BufferedReader;
import java.io.FileReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class SetupChatTables {
    public static void main(String[] args) {
        String host = System.getenv("DB_HOST");
        String dbName = System.getenv("DB_NAME");
        String username = System.getenv("DB_USERNAME");
        String password = System.getenv("DB_PASSWORD");

        if (host == null || dbName == null || username == null || password == null) {
            System.err.println("❌ Error: Missing environment variables!");
            System.err.println("Please make sure .env file exists with DB_HOST, DB_NAME, DB_USERNAME, DB_PASSWORD");
            System.exit(1);
        }

        String url = "jdbc:mysql://" + host + ":3306/" + dbName +
                    "?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";

        System.out.println("================================");
        System.out.println("SETUP CHAT TABLES FOR ONLINE DB");
        System.out.println("================================");
        System.out.println("");
        System.out.println("Connecting to: " + url);
        System.out.println("Username: " + username);
        System.out.println("");

        try (Connection conn = DriverManager.getConnection(url, username, password)) {
            System.out.println("✓ Connected successfully!");
            System.out.println("");
            System.out.println("Creating chat_conversations table...");

            Statement stmt = conn.createStatement();

            // Create chat_conversations table
            String createConversationsTable =
                "CREATE TABLE IF NOT EXISTS chat_conversations (" +
                "id VARCHAR(100) PRIMARY KEY," +
                "customer_id BIGINT NOT NULL," +
                "customer_name VARCHAR(100)," +
                "seller_id BIGINT NOT NULL," +
                "seller_name VARCHAR(100)," +
                "last_message TEXT," +
                "last_message_time TIMESTAMP NULL," +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                "UNIQUE KEY uk_customer_seller (customer_id, seller_id)," +
                "INDEX idx_customer (customer_id)," +
                "INDEX idx_seller (seller_id)," +
                "INDEX idx_last_message_time (last_message_time)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci";

            stmt.executeUpdate(createConversationsTable);
            System.out.println("✓ chat_conversations table created!");

            // Create chat_messages table
            System.out.println("Creating chat_messages table...");
            String createMessagesTable =
                "CREATE TABLE IF NOT EXISTS chat_messages (" +
                "message_id BIGINT PRIMARY KEY AUTO_INCREMENT," +
                "conversation_id VARCHAR(100) NOT NULL," +
                "sender_id BIGINT NOT NULL," +
                "sender_name VARCHAR(100)," +
                "sender_role VARCHAR(20)," +
                "receiver_id BIGINT NOT NULL," +
                "content TEXT NOT NULL," +
                "message_type VARCHAR(20) DEFAULT 'TEXT'," +
                "is_read BOOLEAN DEFAULT FALSE," +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "INDEX idx_conversation (conversation_id)," +
                "INDEX idx_sender (sender_id)," +
                "INDEX idx_receiver (receiver_id)," +
                "INDEX idx_created_at (created_at)," +
                "INDEX idx_is_read (is_read)," +
                "INDEX idx_unread_messages (conversation_id, receiver_id, is_read)," +
                "INDEX idx_conversation_messages (conversation_id, created_at)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci";

            stmt.executeUpdate(createMessagesTable);
            System.out.println("✓ chat_messages table created!");

            System.out.println("");
            System.out.println("================================");
            System.out.println("✓ Setup completed successfully!");
            System.out.println("================================");
            System.out.println("");
            System.out.println("Now you can run the application with:");
            System.out.println("  ./run-with-online-db.sh");
            System.out.println("");

            stmt.close();
        } catch (Exception e) {
            System.err.println("❌ Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}

