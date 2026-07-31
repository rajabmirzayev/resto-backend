package az.codlab;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DbMigrationApplication {

    public static void main(String[] args) {
        var context = SpringApplication.run(DbMigrationApplication.class, args);
        System.exit(SpringApplication.exit(context));
    }

}