package pt.hotelbooking.identity;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import pt.hotelbooking.identity.config.IdentityBootstrapProperties;

@SpringBootApplication
@EnableConfigurationProperties(IdentityBootstrapProperties.class)
public class IdentityServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(IdentityServiceApplication.class, args);
    }
}
