package com.example.gateway.integration;

import com.example.gateway.idempotency.RedisIdempotencyService;
import com.example.gateway.payment.PaymentRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest
class TestcontainersPaymentIT {

    @Container
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("gateway")
                    .withUsername("gateway")
                    .withPassword("gateway");

    @Container
    static GenericContainer<?> redis =
            new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port",
                () -> redis.getMappedPort(6379));
    }

    @Autowired PaymentRepository payments;
    @Autowired RedisIdempotencyService idempotency;

    @Test
    void postgresAndRedisAreAvailable() {
        assertThat(postgres.isRunning()).isTrue();
        assertThat(redis.isRunning()).isTrue();
        assertThat(idempotency.acquire("tc-key")).isTrue();
        assertThat(idempotency.acquire("tc-key")).isFalse();
    }
}
