package com.gregomebije.wallet;

import com.gregomebije.wallet.dto.Models.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import com.gregomebije.wallet.integration.LedgerClient;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * End-to-end test: 
 * Requires Docker running locally.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                // WebClientConfig's @Value("${ledger.service.url}") points back at this same app.
                "ledger.service.url=http://localhost:8081",
                "payment.gateway.url=http://localhost:8082" // unused here, but must resolve
        })
@Testcontainers
class LedgerClientTest {

    @Container
    @ServiceConnection // auto-wires spring.datasource.* to the container
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    @Autowired
    LedgerClient ledgerClient; // real bean, real WebClientConfig, real HTTP

    private String email;
    private String tenantId;
    private String token;

    /** Fresh user + tenant + token for every test, so tests never collide with each other. */
    @BeforeEach
    void onboard() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        email = "user-" + suffix + "@mangital.com";
        tenantId = "tenant-" + suffix;

        ledgerClient.registerUser(new UserRequest(email, "password", "John", "Doe"))
                .block(TIMEOUT);
        ledgerClient.registerTenant(new TenantRequest(tenantId, "Mangital ltd", email))
                .block(TIMEOUT);
        token = ledgerClient.login(new LoginRequest(tenantId, email, "password"))
                .block(TIMEOUT);
    }

    // ---------- Happy path ----------

    @Test
    @DisplayName("onboarding -> login -> create accounts -> post balanced transaction")
    void fullFlow() {
        assertThat(token).isNotBlank();

        String aliceId = createAccount("Alice wallet");
        String fundingId = createAccount("Funding account");

        String result = ledgerClient
                .postTransaction(token, balancedTx("fund-" + UUID.randomUUID(), fundingId, aliceId))
                .block(TIMEOUT);

        assertThat(result).startsWith("Ledger entry balance block recorded successfully:");
    }

    // ---------- Onboarding errors ----------

    @Test
    @DisplayName("registerUser -> duplicate email -> 400")
    void registerUser_duplicateEmail() {
        assertThatThrownBy(() ->
                ledgerClient.registerUser(new UserRequest(email, "password", "John", "Doe"))
                        .block(TIMEOUT))
                .isInstanceOfSatisfying(WebClientResponseException.class, ex -> {
                    assertThat(ex.getStatusCode().value()).isEqualTo(400);
                    assertThat(ex.getResponseBodyAsString()).contains("Email already exist.");
                });
    }

    @Test
    @DisplayName("login -> wrong password -> 4xx")
    void login_wrongPassword() {
        assertThatThrownBy(() ->
                ledgerClient.login(new LoginRequest(tenantId, email, "wrong-password"))
                        .block(TIMEOUT))
                .isInstanceOfSatisfying(WebClientResponseException.class,
                        ex -> assertThat(ex.getStatusCode().is4xxClientError()).isTrue());
    }

    // ---------- Security ----------

    @Test
    @DisplayName("createAccount -> no token -> 401")
    void createAccount_noToken() {
        assertThatThrownBy(() ->
                ledgerClient.createAccount(null, newAccount("No token account")).block(TIMEOUT))
                .isInstanceOfSatisfying(WebClientResponseException.class,
                        ex -> assertThat(ex.getStatusCode().value()).isEqualTo(401));
    }

    // ---------- Ledger rules ----------

    @Test
    @DisplayName("postTransaction -> unbalanced -> 400")
    void postTransaction_unbalanced() {
        String aliceId = createAccount("Alice wallet");
        String fundingId = createAccount("Funding account");

        TransactionRequest unbalanced = new TransactionRequest(
                "Unbalanced", "unbalanced-" + UUID.randomUUID(),
                List.of(
                        new LedgerLine(fundingId, "DEBIT", new BigDecimal("100000"), BigDecimal.ZERO),
                        new LedgerLine(aliceId, "CREDIT", BigDecimal.ZERO, new BigDecimal("50000"))));

        assertThatThrownBy(() -> ledgerClient.postTransaction(token, unbalanced).block(TIMEOUT))
                .isInstanceOfSatisfying(WebClientResponseException.class,
                        ex -> assertThat(ex.getStatusCode().value()).isEqualTo(400));
    }

    @Test
    @DisplayName("postTransaction -> same idempotency key twice -> 409")
    void postTransaction_idempotencyConflict() {
        String aliceId = createAccount("Alice wallet");
        String fundingId = createAccount("Funding account");
        TransactionRequest tx = balancedTx("dup-" + UUID.randomUUID(), fundingId, aliceId);

        ledgerClient.postTransaction(token, tx).block(TIMEOUT); // first call succeeds

        assertThatThrownBy(() -> ledgerClient.postTransaction(token, tx).block(TIMEOUT))
                .isInstanceOfSatisfying(WebClientResponseException.class,
                        ex -> assertThat(ex.getStatusCode().value()).isEqualTo(409));
    }

    // ---------- Helpers ----------

    private CreateAccountRequest newAccount(String name) {
        // random account number so repeated runs never clash
        String number = String.valueOf(1000 + (int) (Math.random() * 9000))
                + UUID.randomUUID().toString().substring(0, 4);
        return new CreateAccountRequest(number, name, "ASSET", "USD");
    }

    /**
     * Creates an account and extracts its id from "Account created: <id>".
     * ASSUMPTION: the id after the colon is the account id usable in ledger lines.
     * Adjust here if your API returns something different.
     */
    private String createAccount(String name) {
        String response = ledgerClient.createAccount(token, newAccount(name)).block(TIMEOUT);
        assertThat(response).startsWith("Account created:");
        return response.substring(response.indexOf(':') + 1).trim();
    }

    private static CreateTransactionRequest balancedTx(String idempotencyKey, String debitAccountId,
                                                 String creditAccountId) {
        return new CreateTransactionRequest(
                "Fund Alice", idempotencyKey,
                List.of(
                        new LedgerLine(debitAccountId, "DEBIT", new BigDecimal("100000"), BigDecimal.ZERO),
                        new LedgerLine(creditAccountId, "CREDIT", BigDecimal.ZERO, new BigDecimal("100000"))));
    }
}