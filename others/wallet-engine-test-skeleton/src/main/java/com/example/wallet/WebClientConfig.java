package com.gregomebije.wallet.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;
import java.time.Duration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;


@Configuration
public class WebClientConfig {

    @Value("${ledger.service.url}")
    private String ledgerServiceUrl;

    @Value("${payment.gateway.url}")
    private String paymentGatewayUrl;

    @Bean
    public WebClient ledgerWebClient() {
        return WebClient.builder()
                .baseUrl(ledgerServiceUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Bean
    public WebClient paymentGatewayWebClientP() {
        return WebClient.builder()
                .baseUrl(paymentGatewayUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }
}