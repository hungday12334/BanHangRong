package banhangrong.su25.Util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DatabaseUpdater implements CommandLineRunner {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) throws Exception {
        try {
            // Kiểm tra xem cột order_item_id đã tồn tại chưa
            String checkColumnQuery = "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS " +
                    "WHERE TABLE_NAME = 'product_reviews' AND COLUMN_NAME = 'order_item_id'";
            
            Integer columnExists = jdbcTemplate.queryForObject(checkColumnQuery, Integer.class);
            
            if (columnExists == null || columnExists == 0) {
                System.out.println("Updating database schema for review system...");
                
                // Thêm cột order_item_id
                jdbcTemplate.execute("ALTER TABLE product_reviews ADD COLUMN order_item_id BIGINT");
                System.out.println("Added order_item_id column");
                
                // Thêm foreign key constraint
                jdbcTemplate.execute("ALTER TABLE product_reviews ADD CONSTRAINT fk_reviews_order_item " +
                        "FOREIGN KEY (order_item_id) REFERENCES order_items(order_item_id)");
                System.out.println("Added foreign key constraint");
                
                // Thêm index
                jdbcTemplate.execute("CREATE INDEX idx_reviews_order_item ON product_reviews(order_item_id)");
                System.out.println("Added index for order_item_id");
                
                // Thêm index cho product và created_at
                jdbcTemplate.execute("CREATE INDEX idx_reviews_product_created ON product_reviews(product_id, created_at)");
                System.out.println("Added index for product_id and created_at");
                
                // Xóa constraint cũ nếu tồn tại
                try {
                    jdbcTemplate.execute("ALTER TABLE product_reviews DROP CONSTRAINT ux_reviews_user_product");
                    System.out.println("Removed old unique constraint");
                } catch (Exception e) {
                    System.out.println("Old constraint not found or already removed");
                }
                
                // Thêm constraint mới
                jdbcTemplate.execute("ALTER TABLE product_reviews ADD CONSTRAINT ux_reviews_order_item " +
                        "UNIQUE (order_item_id)");
                System.out.println("Added new unique constraint for order_item_id");
                
                System.out.println("Database schema updated successfully!");
            } else {
                System.out.println("Database schema already up to date");
            }
            
        } catch (Exception e) {
            System.err.println("Error updating database schema: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
