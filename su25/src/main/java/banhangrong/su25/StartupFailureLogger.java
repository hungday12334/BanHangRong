package banhangrong.su25;

import org.springframework.boot.context.event.ApplicationFailedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Component
public class StartupFailureLogger implements ApplicationListener<ApplicationFailedEvent> {
    private static final String RED = "\u001B[31m";
    private static final String RESET = "\u001B[0m";

    @Override
    public void onApplicationEvent(@NonNull ApplicationFailedEvent event) {
        System.out.println("\n==== Startup Status ====");
        System.out.println(RED + "[FAIL] Application failed to start" + RESET);
        System.out.println(RED + "[FAIL] Web server not running" + RESET);
        System.out.println(RED + "[FAIL] Database connection could not be verified" + RESET);
        System.out.println("=======================\n");
    }
}
