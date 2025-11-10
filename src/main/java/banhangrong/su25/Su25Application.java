package banhangrong.su25;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Su25Application {
    public static void main(String[] args) {
        SpringApplication.run(Su25Application.class, args);
        System.out.println("Press ENTER to start http://localhost:8080 (app is ready)");
    }
}
