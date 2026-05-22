package com.upc.backend_techstore.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CheckoutRequestDto implements Serializable {
    private Long carritoId;

    // Datos de pago Mercado Pago
    @JsonProperty("token")
    private String token;

    @JsonProperty("issuer_id")
    private String issuerId;

    @JsonProperty("payment_method_id")
    private String paymentMethodId;

    @JsonProperty("transaction_amount")
    private BigDecimal transactionAmount;

    private Integer installments;

    private String description;

    @JsonProperty("payer")
    private PayerDto payer;

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class PayerDto implements Serializable {
        private String email;
    }
}

