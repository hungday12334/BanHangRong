package banhangrong.su25;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;

@SpringBootApplication
public class Su25Application {
    public static void main(String[] args) {
        ConfigurableApplicationContext ctx = SpringApplication.run(Su25Application.class, args);
        Environment env = ctx.getEnvironment();
        String port = env.getProperty("local.server.port", env.getProperty("server.port", "8080"));
        String url = "http://localhost:" + port ;
        System.out.println("Enter to star: " + url);
    }
}
