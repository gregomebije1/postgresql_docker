package com.gregomebije.wallet;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class WalletEngineIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    static WireMockServer wireMockServer;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @BeforeAll
    static void startMocks() {
        wireMockServer = new WireMockServer(0); // Dynamic port
        wireMockServer.start();
        WireMock.configureFor("localhost", wireMockServer.port());
    }

    @AfterAll
    static void stopMocks() {
        wireMockServer.stop();
    }

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void testWalletFundingEndpointWithMockedGateway() {
        // Mock external Payment Gateway endpoint
        wireMockServer.stubFor(WireMock.post(WireMock.urlEqualTo("/api/v1/charges"))
                .willReturn(WireMock.aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"status\":\"SUCCESS\",\"referenceId\":\"pg_ref_999\"}")));

        // Perform HTTP request to Wallet Engine
        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/wallets/fund?customerId=cust_A&amount=50.00", null, String.class);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
    }

     @Test
    void testFundWalletWorkflow() {
        // Mock Payment Gateway response
        wireMockServer.stubFor(WireMock.post(WireMock.urlEqualTo("/api/v1/charges"))
                .willReturn(WireMock.aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"status\":\"SUCCESS\",\"gatewayReference\":\"gw_ref_123\"}")));

        // Execute Wallet Funding Request
        FundRequest request = new FundRequest("cust_A", 100.00, "USD", "key_abc");
        ResponseEntity<WalletResponse> response = restTemplate.postForEntity(
                "/api/wallets/fund", request, WalletResponse.class);

        org.assertj.core.api.Assertions.assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
    }
}
