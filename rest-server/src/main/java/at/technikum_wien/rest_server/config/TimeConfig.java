package at.technikum_wien.rest_server.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class TimeConfig {

    // Injected instead of calling now() directly, so expiry logic can be tested with a fixed clock
    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
