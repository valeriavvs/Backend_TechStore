package com.upc.backend_techstore.services;

import com.mercadopago.MercadoPagoConfig;
import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.client.payment.PaymentCreateRequest;
import com.mercadopago.client.payment.PaymentPayerRequest;
import com.mercadopago.resources.payment.Payment;
import com.upc.backend_techstore.dto.PaymentRequestDto;
import com.upc.backend_techstore.dto.PaymentResponseDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import jakarta.annotation.PostConstruct;
import java.math.BigDecimal;

@Slf4j
@Service
public class PaymentService {

    private PaymentClient paymentClient;

    @Value("${mercadopago.access-token:}")
    private String accessToken;

    @PostConstruct
    public void init() {
        if (hasText(accessToken)) {
            MercadoPagoConfig.setAccessToken(accessToken);
            paymentClient = new PaymentClient();
            log.info("Mercado Pago configurado correctamente");
        } else {
            log.warn("MERCADOPAGO_ACCESS_TOKEN no configurado. El endpoint /api/payments no funcionará hasta definir la variable de entorno.");
        }
    }

    public PaymentResponseDto processCardPayment(PaymentRequestDto request) {
        validateRequest(request);
        ensureConfigured();

        String tokenPrefix = tokenPrefix(request.getToken());
        String email = request.getPayer() != null ? request.getPayer().getEmail() : null;
        log.info("Procesando pago con Mercado Pago. email={}, amount={}, installments={}, method={}, tokenPrefix={}",
                email,
                request.getTransactionAmount(),
                request.getInstallments(),
                request.getPaymentMethodId(),
                tokenPrefix);

        try {
            var builder = PaymentCreateRequest.builder()
                    .transactionAmount(request.getTransactionAmount())
                    .token(request.getToken())
                    .installments(request.getInstallments())
                    .description(request.getDescription())
                    .paymentMethodId(request.getPaymentMethodId())
                    .payer(PaymentPayerRequest.builder()
                            .email(email)
                            .build());

            if (hasText(request.getIssuerId())) {
                builder.issuerId(request.getIssuerId());
            }

            Payment payment = paymentClient.create(builder.build());
            log.info("Pago creado en Mercado Pago. id={}, status={}, statusDetail={}, metodo={}",
                    payment.getId(), payment.getStatus(), payment.getStatusDetail(), payment.getPaymentMethodId());
            return mapToResponse(payment);
        } catch (Exception ex) {
            log.error("Error procesando pago con Mercado Pago. email={}, amount={}, method={}, tokenPrefix={}, error={}",
                    email,
                    request.getTransactionAmount(),
                    request.getPaymentMethodId(),
                    tokenPrefix,
                    ex.getMessage(), ex);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "No se pudo procesar el pago con Mercado Pago");
        }
    }

    private void validateRequest(PaymentRequestDto request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El body del pago es obligatorio");
        }
        if (!hasText(request.getToken())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El token de la tarjeta es obligatorio");
        }
        if (!hasText(request.getPaymentMethodId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El payment_method_id es obligatorio");
        }
        if (request.getTransactionAmount() == null || request.getTransactionAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El monto debe ser mayor a cero");
        }
        if (request.getInstallments() == null || request.getInstallments() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Las cuotas deben ser mayores a cero");
        }
        if (request.getPayer() == null || !hasText(request.getPayer().getEmail())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El email del payer es obligatorio");
        }
        if (!hasText(request.getDescription())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La descripción del pago es obligatoria");
        }
    }

    private void ensureConfigured() {
        if (paymentClient == null) {
            if (!hasText(accessToken)) {
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Mercado Pago no está configurado en el backend");
            }
            MercadoPagoConfig.setAccessToken(accessToken);
            paymentClient = new PaymentClient();
        }
    }

    private PaymentResponseDto mapToResponse(Payment payment) {
        PaymentResponseDto response = new PaymentResponseDto();
        response.setIdPago(payment.getId());
        response.setStatus(payment.getStatus());
        response.setStatusDetail(payment.getStatusDetail());
        response.setMonto(payment.getTransactionAmount());
        response.setFecha(payment.getDateCreated());
        response.setMetodoPago(payment.getPaymentMethodId());
        return response;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String tokenPrefix(String token) {
        if (!hasText(token)) {
            return "";
        }
        int end = Math.min(10, token.length());
        return token.substring(0, end);
    }
}


