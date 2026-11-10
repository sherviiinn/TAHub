package ir.TAHub.TAHub;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Tests run against a real, empty PostgreSQL 16 that Testcontainers starts in Docker (Docker Desktop must be on).
 * @ServiceConnection gives its address, user and password to Spring, so the tests never touch the
 * development database (tahub-db on port 5433).
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgresContainer() {
        return new PostgreSQLContainer("postgres:16");
    }
}
