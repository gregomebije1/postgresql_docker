@Component
public class PaymentGatewayClient {

    private final WebClient paymentGatewayWebClient;

    public PaymentGatewayClient(WebClient paymentGatewayWebClient) {
        this.paymentGatewayWebClient = paymentGatewayWebClient;
    }

    public Mono<PaymentResponse> charge(ChargeRequest request, String idempotencyKey) {
        return paymentGatewayWebClient.post()
                .uri("/api/v1/charges")
                .header("Idempotency-Key", idempotencyKey)
                .bodyValue(request)
                .retrieve()
                .onStatus(HttpStatusCode::isError, response -> 
                    response.bodyToMono(String.class)
                            .flatMap(errorBody -> Mono.error(new PaymentGatewayException("Payment failed: " + errorBody)))
                )
                .bodyToMono(PaymentResponse.class);
    }
}