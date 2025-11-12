package banhangrong.su25;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableAsync //Cho phep dung anotation Async de chay bat dong bo
public class Su25Application {
    public static void main(String[] args) {
        SpringApplication.run(Su25Application.class, args);
        System.out.println("Website is running at http://localhost:8080");
    }
}
