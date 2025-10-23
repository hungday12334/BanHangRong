package banhangrong.su25.Util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

@Component
public class DatabaseUpdater implements CommandLineRunner {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) throws Exception {
        try {
            System.out.println("Setting up complete review system database...");
            
            // Đọc script SQL từ file sql/review_system_complete.sql
            String sqlScript = new String(Files.readAllBytes(Paths.get("sql/review_system_complete.sql")));
            
            // Chia script thành các câu lệnh riêng biệt
            String[] statements = sqlScript.split(";");
            
            for (String statement : statements) {
                statement = statement.trim();
                if (!statement.isEmpty() && !statement.startsWith("--") && !statement.startsWith("/*")) {
                    try {
                        jdbcTemplate.execute(statement);
                        System.out.println("Executed: " + statement.substring(0, Math.min(50, statement.length())) + "...");
                    } catch (Exception e) {
                        System.out.println("Skipped (already exists or error): " + statement.substring(0, Math.min(50, statement.length())) + "...");
                    }
                }
            }
            
            System.out.println("Complete review system database setup completed successfully!");
            
        } catch (IOException e) {
            System.err.println("Error reading SQL file: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("Error setting up review system database: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
