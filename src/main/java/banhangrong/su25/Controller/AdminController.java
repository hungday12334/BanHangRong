package banhangrong.su25.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/admin")
public class AdminController {
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @GetMapping("/setup-review-system")
    @org.springframework.web.bind.annotation.ResponseBody
    public ResponseEntity<Map<String, Object>> setupReviewSystem() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            System.out.println("Setting up complete review system...");
            
            // Đọc và thực thi script SQL tổng hợp
            String sqlScript = new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get("sql/review_system_complete.sql")));
            
            // Chia script thành các câu lệnh riêng biệt
            String[] statements = sqlScript.split(";");
            int executedCount = 0;
            int skippedCount = 0;
            
            for (String statement : statements) {
                statement = statement.trim();
                if (!statement.isEmpty() && !statement.startsWith("--") && !statement.startsWith("/*")) {
                    try {
                        jdbcTemplate.execute(statement);
                        executedCount++;
                        System.out.println("Executed: " + statement.substring(0, Math.min(50, statement.length())) + "...");
                    } catch (Exception e) {
                        skippedCount++;
                        System.out.println("Skipped: " + statement.substring(0, Math.min(50, statement.length())) + "...");
                    }
                }
            }
            
            response.put("success", true);
            response.put("message", "Review system setup completed successfully!");
            response.put("executed_statements", executedCount);
            response.put("skipped_statements", skippedCount);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error setting up review system: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    @GetMapping("/fix-old-constraint")
    @org.springframework.web.bind.annotation.ResponseBody
    public ResponseEntity<Map<String, Object>> fixOldConstraint() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            System.out.println("Fixing old constraint...");
            
            // Tạm thời disable foreign key checks
            jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 0");
            
            // Xóa constraint cũ
            try {
                jdbcTemplate.execute("ALTER TABLE product_reviews DROP CONSTRAINT ux_reviews_user_product");
                System.out.println("Old constraint dropped successfully");
            } catch (Exception e) {
                System.out.println("Old constraint not found or already dropped");
            }
            
            // Bật lại foreign key checks
            jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 1");
            
            // Kiểm tra và thêm constraint mới (chỉ nếu chưa tồn tại)
            String checkConstraint = """
                SELECT COUNT(*) 
                FROM information_schema.table_constraints 
                WHERE table_schema = DATABASE() 
                AND table_name = 'product_reviews' 
                AND constraint_name = 'ux_reviews_order_item'
                """;
            
            Integer constraintExists = jdbcTemplate.queryForObject(checkConstraint, Integer.class);
            
            if (constraintExists == null || constraintExists == 0) {
                try {
                    jdbcTemplate.execute("ALTER TABLE product_reviews ADD CONSTRAINT ux_reviews_order_item UNIQUE (order_item_id)");
                    System.out.println("New constraint added successfully");
                } catch (Exception e) {
                    System.out.println("Error adding new constraint: " + e.getMessage());
                }
            } else {
                System.out.println("New constraint already exists");
            }
            
            response.put("success", true);
            response.put("message", "Old constraint fixed successfully!");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error fixing old constraint: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    @GetMapping("/check-review-status")
    @org.springframework.web.bind.annotation.ResponseBody
    public ResponseEntity<Map<String, Object>> checkReviewStatus() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            System.out.println("Checking review system status...");
            
            // Kiểm tra constraints
            String checkConstraints = """
                SELECT 
                    CONSTRAINT_NAME,
                    CONSTRAINT_TYPE
                FROM information_schema.table_constraints 
                WHERE table_schema = DATABASE() 
                AND table_name = 'product_reviews'
                """;
            
            var constraints = jdbcTemplate.queryForList(checkConstraints);
            response.put("constraints", constraints);
            
            // Kiểm tra indexes
            String checkIndexes = """
                SELECT 
                    INDEX_NAME,
                    COLUMN_NAME
                FROM information_schema.statistics 
                WHERE table_schema = DATABASE() 
                AND table_name = 'product_reviews'
                """;
            
            var indexes = jdbcTemplate.queryForList(checkIndexes);
            response.put("indexes", indexes);
            
            // Kiểm tra triggers
            String checkTriggers = """
                SELECT 
                    TRIGGER_NAME,
                    EVENT_MANIPULATION
                FROM information_schema.triggers 
                WHERE table_schema = DATABASE() 
                AND table_name = 'product_reviews'
                """;
            
            var triggers = jdbcTemplate.queryForList(checkTriggers);
            response.put("triggers", triggers);
            
            // Kiểm tra dữ liệu
            String checkData = """
                SELECT 
                    COUNT(*) as total_reviews,
                    COUNT(DISTINCT product_id) as products_with_reviews
                FROM product_reviews
                """;
            
            var data = jdbcTemplate.queryForMap(checkData);
            response.put("data", data);
            
            response.put("success", true);
            response.put("message", "Review system status checked successfully!");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error checking review status: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
}
