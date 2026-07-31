package az.codlab.setting;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class SettingServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(SettingServiceApplication.class, args);
    }

}
