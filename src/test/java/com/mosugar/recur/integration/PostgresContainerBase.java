package com.mosugar.recur.integration;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Shared Testcontainers base: spins up one PostgreSQL container per test class.
 * Subclasses annotate with @SpringBootTest (or @DataJpaTest) as needed.
 */
@Testcontainers
public abstract class PostgresContainerBase {

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:17-alpine")
                    .withDatabaseName("recur_test")
                    .withUsername("recur")
                    .withPassword("recur");

    @DynamicPropertySource
    static void overrideDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        // @DataJpaTest is a slice — Flyway is not in the context.
        // Let Hibernate build the schema from entities instead.
        registry.add("spring.flyway.enabled", () -> "false");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        // JWT secret is required by SecurityConfig even in slice tests
        registry.add("app.jwt.secret",
                () -> "test-secret-key-for-testing-only-must-be-long-enough-for-hs256");
    }
}
