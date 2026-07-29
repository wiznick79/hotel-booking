package pt.hotelbooking;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication(scanBasePackages = {
        "pt.hotelbooking.hotel",
        "pt.hotelbooking.core"
})
@EntityScan("pt.hotelbooking.hotel.model.entity")
@EnableJpaRepositories("pt.hotelbooking.hotel.repository")
@EnableCaching
public class HotelServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(HotelServiceApplication.class, args);
    }
}
