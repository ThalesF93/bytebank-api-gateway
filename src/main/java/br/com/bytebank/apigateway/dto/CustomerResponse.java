package br.com.bytebank.apigateway.dto;

import java.util.UUID;

public record CustomerResponse(
        UUID uuid,
        String name,
        String phone
) {
}
