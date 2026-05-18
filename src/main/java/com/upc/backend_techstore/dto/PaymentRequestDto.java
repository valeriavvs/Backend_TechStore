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
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequestDto implements Serializable {

    @JsonProperty("token")
    private String token;

    @JsonProperty("issuer_id")
    private String issuerId;

    @JsonProperty("payment_method_id")
    private String paymentMethodId;

    @JsonProperty("transaction_amount")
    private BigDecimal transactionAmount;

    @JsonProperty("installments")
    private Integer installments;

    @JsonProperty("description")
    private String description;

    @JsonProperty("payer")
    private PayerDto payer;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PayerDto implements Serializable {

        @JsonProperty("email")
        private String email;
    }
}

