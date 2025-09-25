package banhangrong.su25;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.lang.NonNull;

import javax.sql.DataSource;

@Component
public class StartupStatusLogger implements ApplicationListener<ApplicationReadyEvent> {
    private static final Logger log = LoggerFactory.getLogger(StartupStatusLogger.class);
    private static final String GREEN = "\u001B[32m";
    private static final String RED = "\u001B[31m";
    private static final String RESET = "\u001B[0m";

    @Autowired(required = false)
    private DataSource dataSource;

    @Autowired
    private Environment env;

    @Override
    public void onApplicationEvent(@NonNull ApplicationReadyEvent event) {
        boolean dbOk = checkDb();
        boolean webOk = checkWeb();

        String dbMsg = dbOk ? GREEN + "[OK] Database connected" + RESET
                : RED + "[FAIL] Database connection failed" + RESET;
        String webMsg = webOk ? GREEN + "[OK] Web server started at http://localhost:" + env.getProperty("local.server.port") + RESET
                : RED + "[FAIL] Web server failed to start" + RESET;

        // Print concise status lines
        System.out.println("\n==== Startup Status ====");
        System.out.println(dbMsg);
        System.out.println(webMsg);
        System.out.println("=======================\n");
    }

    private boolean checkDb() {
        if (dataSource == null) return false;
        try {
            // Use a quick validation query
            JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            return true;
        } catch (Exception e) {
            log.debug("DB check error", e);
            return false;
        }
    }

    private boolean checkWeb() {
        // If the app reached ApplicationReadyEvent, the web server is up.
        // Additionally check that local.server.port is set.
        String port = env.getProperty("local.server.port");
        return port != null && !port.isBlank();
    }
}
