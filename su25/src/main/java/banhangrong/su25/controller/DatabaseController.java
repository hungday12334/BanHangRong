package banhangrong.su25.Controller;

import org.springframework.jdbc.core.ColumnMapRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Controller
public class DatabaseController {

    private final JdbcTemplate jdbcTemplate;

    public DatabaseController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public record TableData(
            String name,
            long count,
            List<String> columns,
            List<Map<String, Object>> rows
    ) {}

    @GetMapping("/db")
    public String inspectDatabase(@RequestParam(name = "limit", defaultValue = "10") int limit,
                                  Model model) {
        int safeLimit = Math.min(Math.max(limit, 1), 100);

        // Get current schema/database name
        String schema = jdbcTemplate.queryForObject("SELECT DATABASE()", String.class);

        // Fetch all base tables in the current schema
        List<String> tables = jdbcTemplate.queryForList(
                "SELECT table_name FROM information_schema.tables WHERE table_schema = ? AND table_type = 'BASE TABLE' ORDER BY table_name",
                String.class,
                schema
        );

        List<TableData> tableDataList = new ArrayList<>();

        for (String table : tables) {
            // Get ordered columns for consistent display
            List<String> columns = jdbcTemplate.queryForList(
                    "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ? ORDER BY ORDINAL_POSITION",
                    String.class,
                    schema,
                    table
            );

            long count = 0L;
            try {
                Long c = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM `" + table + "`", Long.class);
                count = c != null ? c : 0L;
            } catch (Exception ignored) {
                // Some tables may not be readable by the current user
            }

            List<Map<String, Object>> rows = List.of();
            try {
                rows = jdbcTemplate.query("SELECT * FROM `" + table + "` LIMIT " + safeLimit, new ColumnMapRowMapper());
            } catch (Exception ignored) {
                // Skip preview if cannot select
            }

            tableDataList.add(new TableData(table, count, columns, rows));
        }

        model.addAttribute("schema", schema);
        model.addAttribute("tableCount", tables.size());
        model.addAttribute("limit", safeLimit);
        model.addAttribute("tables", tableDataList);

        return "db";
    }

    @GetMapping("/api/db")
    @ResponseBody
    public Map<String, Object> inspectDatabaseJson(@RequestParam(name = "limit", defaultValue = "10") int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 100);

        String schema = jdbcTemplate.queryForObject("SELECT DATABASE()", String.class);

        List<String> tables = jdbcTemplate.queryForList(
                "SELECT table_name FROM information_schema.tables WHERE table_schema = ? AND table_type = 'BASE TABLE' ORDER BY table_name",
                String.class,
                schema
        );

        List<TableData> tableDataList = new ArrayList<>();
        for (String table : tables) {
            List<String> columns = jdbcTemplate.queryForList(
                    "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ? ORDER BY ORDINAL_POSITION",
                    String.class,
                    schema,
                    table
            );

            long count = 0L;
            try {
                Long c = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM `" + table + "`", Long.class);
                count = c != null ? c : 0L;
            } catch (Exception ignored) {}

            List<Map<String, Object>> rows = List.of();
            try {
                rows = jdbcTemplate.query("SELECT * FROM `" + table + "` LIMIT " + safeLimit, new ColumnMapRowMapper());
            } catch (Exception ignored) {}

            tableDataList.add(new TableData(table, count, columns, rows));
        }

        return Map.of(
                "schema", schema,
                "tableCount", tables.size(),
                "limit", safeLimit,
                "tables", tableDataList
        );
    }
}
