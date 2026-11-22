package at.technikum_wien.rest_server.e2e;

import at.technikum_wien.rest_server.RestServerApplication;
import org.springframework.boot.SpringApplication;

/**
 * Runs the server without Docker, e.g. to try the desktop app:
 * ./mvnw spring-boot:test-run -Dspring-boot.run.main-class=at.technikum_wien.rest_server.e2e.E2eServer
 */
public class E2eServer {

    public static void main(String[] args) {
        SpringApplication.from(RestServerApplication::main)
                .with(InMemoryInfrastructure.class)
                .withAdditionalProfiles("e2e")
                .run(args);
    }
}
