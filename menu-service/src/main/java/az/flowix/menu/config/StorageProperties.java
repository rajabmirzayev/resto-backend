package az.flowix.menu.config;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@Getter
@Setter
@ConfigurationProperties(prefix = "app.storage")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class StorageProperties {

    String baseDir = "./data/menu-images";

    String publicBaseUrl = "http://localhost:8001";

}
