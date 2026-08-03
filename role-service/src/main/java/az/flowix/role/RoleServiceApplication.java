package az.flowix.role;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class RoleServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(RoleServiceApplication.class, args);
    }

}
