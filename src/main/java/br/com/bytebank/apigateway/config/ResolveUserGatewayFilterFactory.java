package br.com.bytebank.apigateway.config;

import org.springframework.cloud.client.loadbalancer.reactive.ReactorLoadBalancerExchangeFilterFunction;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class ResolveUserGatewayFilterFactory extends AbstractGatewayFilterFactory<Object> {

    private final WebClient webClient;

    public ResolveUserGatewayFilterFactory(WebClient.Builder builder,
                                           ReactorLoadBalancerExchangeFilterFunction lbFunction) {
        this.webClient = builder
                .filter(lbFunction)
                .baseUrl("http://bytebank-customer")
                .build();
    }

    @Override
    public GatewayFilter apply(Object config) {
        return (exchange, chain) -> {

            String phoneNumber = exchange.getRequest().getHeaders().getFirst("X-Phone-Number");

            if (phoneNumber == null) {
                exchange.getResponse().setStatusCode(HttpStatus.BAD_REQUEST);
                return exchange.getResponse().setComplete();
            }

            String phone = phoneNumber
                    .replace("@s.whatsapp.net", "")
                    .replace("@c.us", "");

            if (phone.startsWith("55")) {
                phone = phone.substring(2);
            }

            return webClient.get()
                    .uri("/api/v2/customers/phone/{phone}", phone)
                    .retrieve()
                    .bodyToMono(String.class)
                    .flatMap(userId -> {
                        ServerHttpRequest mutatedRequest = exchange.getRequest()
                                .mutate()
                                .header("X-User-Id", userId)
                                .build();

                        return chain.filter(exchange.mutate().request(mutatedRequest).build());
                    });
        };
    }
}