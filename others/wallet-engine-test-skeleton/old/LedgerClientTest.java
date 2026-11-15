package com.gregomebije.wallet;

import com.gregomebije.wallet.integration.LedgerClient;
import com.gregomebije.wallet.config.WebClientConfig;
import com.gregomebije.wallet.integration.LedgerClient.AccountRequest;
import com.gregomebije.wallet.integration.LedgerClient.LedgerLine;
import com.gregomebije.wallet.integration.LedgerClient.LoginRequest;
import com.gregomebije.wallet.integration.LedgerClient.TenantRequest;
import com.gregomebije.wallet.integration.LedgerClient.TransactionRequest;
import com.gregomebije.wallet.integration.LedgerClient.UserRequest;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LedgerClientTest {

    private MockWebServer server;
    private LedgerClient ledgerClient;

    private static final UserRequest USER =
            new UserRequest("johndoe@mangital.com", "password", "John", "Doe");
    private static final TenantRequest TENANT =
            new TenantRequest("tenant-1", "Mangital ltd", "johndoe@mangital.com");
    private static final LoginRequest LOGIN =
            new LoginRequest("tenant-1", "johndoe@mangital.com", "password");
    private static final AccountRequest ACCOUNT =
            new AccountRequest("1001", "Alice wallet", "ASSET", "USD");
    private static final TransactionRequest TX = new TransactionRequest(
            "Fund Alice", "fund-alice-1",
            List.of(
                new LedgerLine("d880a0fe-ce74-4877-98f9-73dbb2ffe46b", "DEBIT",
                        new BigDecimal("100000"), BigDecimal.ZERO),
                new LedgerLine("40eb54c1-291b-4986-a51e-00bf2e99a40e", "CREDIT",
                        BigDecimal.ZERO, new BigDecimal("100000"))));

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();

        // Build the client with the real config class, pointing @Value fields at MockWebServer.
        WebClientConfig config = new WebClientConfig();
        ReflectionTestUtils.setField(config, "ledgerServiceUrl", server.url("/").toString());
        ReflectionTestUtils.setField(config, "paymentGatewayUrl", server.url("/").toString());
        WebClient ledgerWebClient = config.ledgerWebClient();

        ledgerClient = new ClientService(ledgerWebClient);
    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
    }

    // ---------- Onboarding ----------

    @Test
    @DisplayName("registerUser -> 201 and sends correct request")
    void registerUser_created() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(201)
                .setBody("User organization enrolled successfully"));

        StepVerifier.create(ledgerClient.registerUser(USER))
                .expectNext("User organization enrolled successfully")
                .verifyComplete();

        RecordedRequest req = server.takeRequest();
        assertThat(req.getMethod()).isEqualTo("POST");
        assertThat(req.getPath()).isEqualTo("/api/v1/onboarding/user");
        assertThat(req.getHeader("Content-Type")).startsWith(MediaType.APPLICATION_JSON_VALUE);
        assertThat(req.getHeader("Authorization")).isNull();
        assertThat(req.getBody().readUtf8())
                .contains("\"email\":\"johndoe@mangital.com\"")
                .contains("\"firstName\":\"John\"");
    }

    @Test
    @DisplayName("registerUser -> duplicate email -> 400")
    void registerUser_duplicate() {
        server.enqueue(jsonResponse(400, "{\"message\":\"Email already exist.\"}"));

        StepVerifier.create(ledgerClient.registerUser(USER))
                .expectErrorSatisfies(ex -> {
                    assertThat(ex).isInstanceOf(WebClientResponseException.BadRequest.class);
                    assertThat(((WebClientResponseException) ex).getResponseBodyAsString())
                            .contains("Email already exist.");
                })
                .verify();
    }

    @Test
    @DisplayName("registerTenant -> 201")
    void registerTenant_created() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(201)
                .setBody("Tenant added to user organization"));

        StepVerifier.create(ledgerClient.registerTenant(TENANT))
                .expectNext("Tenant added to user organization")
                .verifyComplete();

        RecordedRequest req = server.takeRequest();
        assertThat(req.getPath()).isEqualTo("/api/v1/onboarding/tenant");
        assertThat(req.getBody().readUtf8()).contains("\"tenantId\":\"tenant-1\"");
    }

    @Test
    @DisplayName("login -> 200 -> returns access token")
    void login_ok() throws Exception {
        server.enqueue(jsonResponse(200, "{\"accessToken\":\"jwt.token\"}"));

        StepVerifier.create(ledgerClient.login(LOGIN))
                .expectNext("jwt.token")
                .verifyComplete();

        RecordedRequest req = server.takeRequest();
        assertThat(req.getPath()).isEqualTo("/api/v1/onboarding/login");
        assertThat(req.getBody().readUtf8()).contains("\"password\":\"password\"");
    }

    // ---------- Accounts ----------

    @Test
    @DisplayName("createAccount -> sends Bearer token -> 201")
    void createAccount_created() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(201).setBody("Account created: ACC-1"));

        StepVerifier.create(ledgerClient.createAccount("my-token", ACCOUNT))
                .expectNext("Account created: ACC-1")
                .verifyComplete();

        RecordedRequest req = server.takeRequest();
        assertThat(req.getPath()).isEqualTo("/api/v1/ledgers/accounts");
        assertThat(req.getHeader("Authorization")).isEqualTo("Bearer my-token");
        assertThat(req.getBody().readUtf8())
                .contains("\"accountNumber\":\"1001\"")
                .contains("\"type\":\"ASSET\"");
    }

    @Test
    @DisplayName("createAccount -> invalid token -> 401")
    void createAccount_unauthorized() {
        server.enqueue(new MockResponse().setResponseCode(401));

        StepVerifier.create(ledgerClient.createAccount("bad-token", ACCOUNT))
                .expectError(WebClientResponseException.Unauthorized.class)
                .verify();
    }

    // ---------- Transactions ----------

    @Test
    @DisplayName("postTransaction -> 201 and sends both ledger lines")
    void postTransaction_created() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(201)
                .setBody("Ledger entry balance block recorded successfully: TX-1"));

        StepVerifier.create(ledgerClient.postTransaction("my-token", TX))
                .expectNext("Ledger entry balance block recorded successfully: TX-1")
                .verifyComplete();

        RecordedRequest req = server.takeRequest();
        String body = req.getBody().readUtf8();
        assertThat(req.getPath()).isEqualTo("/api/v1/ledgers/transactions");
        assertThat(req.getHeader("Authorization")).isEqualTo("Bearer my-token");
        assertThat(body)
                .contains("\"idempotencyKey\":\"fund-alice-1\"")
                .contains("\"ledgerLineType\":\"DEBIT\"")
                .contains("\"ledgerLineType\":\"CREDIT\"");
    }

    @Test
    @DisplayName("postTransaction -> unbalanced -> 400")
    void postTransaction_unbalanced() {
        server.enqueue(jsonResponse(400, "{\"message\":\"unbalanced\"}"));

        StepVerifier.create(ledgerClient.postTransaction("my-token", TX))
                .expectErrorSatisfies(ex -> {
                    assertThat(ex).isInstanceOf(WebClientResponseException.BadRequest.class);
                    assertThat(((WebClientResponseException) ex).getResponseBodyAsString())
                            .contains("unbalanced");
                })
                .verify();
    }

    @Test
    @DisplayName("postTransaction -> idempotency breach -> 409")
    void postTransaction_conflict() {
        server.enqueue(new MockResponse().setResponseCode(409));

        StepVerifier.create(ledgerClient.postTransaction("my-token", TX))
                .expectError(WebClientResponseException.Conflict.class)
                .verify();
    }

    @Test
    @DisplayName("postTransaction -> server error -> 500")
    void postTransaction_serverError() {
        server.enqueue(new MockResponse().setResponseCode(500));

        StepVerifier.create(ledgerClient.postTransaction("my-token", TX))
                .expectError(WebClientResponseException.InternalServerError.class)
                .verify();
    }

    private static MockResponse jsonResponse(int status, String body) {
        return new MockResponse()
                .setResponseCode(status)
                .setHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .setBody(body);
    }
}