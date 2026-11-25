package com.gregomebije.wallet.integration;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.List;

/** WebClient-based client for the ledger API. */
@Component
public class LedgerClient {

    public record UserRequest(String email, String password, String firstName, String lastName) {}
    public record TenantRequest(String tenantId, String tenantName, String email) {}
    public record LoginRequest(String tenantId, String email, String password) {}
    public record TokenResponse(String accessToken) {}
    public record AccountRequest(String accountNumber, String name, String type, String currency) {}
    public record LedgerLine(String accountId, String ledgerLineType, BigDecimal debit, BigDecimal credit) {}
    public record TransactionRequest(String description, String idempotencyKey, List<LedgerLine> lines) {}

    private final WebClient ledgerWebClient;

    // Two WebClient beans exist (ledger + payment gateway), so the qualifier is required.
    public LedgerClient(@Qualifier("ledgerWebClient") WebClient ledgerWebClient) {
        this.ledgerWebClient = ledgerWebClient;
    }

    public Mono<String> registerUser(UserRequest request) {
        return post("/api/v1/onboarding/user", request, null);
    }

    public Mono<String> registerTenant(TenantRequest request) {
        return post("/api/v1/onboarding/tenant", request, null);
    }

    public Mono<String> login(LoginRequest request) {
        return ledgerWebClient.post()
                .uri("/api/v1/onboarding/login")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(TokenResponse.class)
                .map(TokenResponse::accessToken);
    }

    public Mono<String> createAccount(String token, AccountRequest request) {
        return post("/api/v1/ledgers/accounts", request, token);
    }

    public Mono<String> postTransaction(String token, TransactionRequest request) {
        return post("/api/v1/ledgers/transactions", request, token);
    }

    private Mono<String> post(String uri, Object body, String token) {
        WebClient.RequestBodySpec spec = ledgerWebClient.post().uri(uri);
        if (token != null) {
            spec = spec.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }
        return spec.bodyValue(body).retrieve().bodyToMono(String.class);
    }
}