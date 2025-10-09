package banhangrong.su25.Controller;

import banhangrong.su25.Repository.UsersRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/database")
public class DatabaseController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UsersRepository usersRepository;

    @PostMapping("/cleanup-duplicate-users")
    public ResponseEntity<?> cleanupDuplicateUsers() {
        try {
            // Đếm số lượng users trước khi xóa
            long countBefore = usersRepository.count();
            
            // SQL để xóa duplicate, giữ lại record có user_id nhỏ nhất
            String deleteSql = """
                DELETE u1 FROM users u1
                INNER JOIN users u2 
                WHERE u1.user_id > u2.user_id 
                AND u1.username = u2.username
                """;
            
            int deleted = jdbcTemplate.update(deleteSql);
            long countAfter = usersRepository.count();
            
            Map<String, Object> result = new HashMap<>();
            result.put("message", "Đã xóa thành công duplicate users");
            result.put("usersBeforeCleanup", countBefore);
            result.put("duplicatesDeleted", deleted);
            result.put("usersAfterCleanup", countAfter);
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Lỗi khi xóa duplicate: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping("/check-duplicate-users")
    public ResponseEntity<?> checkDuplicateUsers() {
        try {
            String checkSql = """
                SELECT username, COUNT(*) as count 
                FROM users 
                GROUP BY username 
                HAVING count > 1
                """;
            
            List<Map<String, Object>> duplicates = jdbcTemplate.queryForList(checkSql);
            
            Map<String, Object> result = new HashMap<>();
            result.put("totalUsers", usersRepository.count());
            result.put("duplicateUsernames", duplicates);
            result.put("hasDuplicates", !duplicates.isEmpty());
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Lỗi khi kiểm tra duplicate: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
}
