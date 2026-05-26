package com.upc.backend_techstore.services;

import com.mercadopago.MercadoPagoConfig;
import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.client.payment.PaymentCreateRequest;
import com.mercadopago.client.payment.PaymentPayerRequest;
import com.mercadopago.resources.payment.Payment;
import com.upc.backend_techstore.dto.PaymentRequestDto;
import com.upc.backend_techstore.dto.PaymentResponseDto;
import lombok.extern.slf4j.Slf4j;
import com.mercadopago.exceptions.MPApiException;
import java.lang.reflect.Method;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
        log.info("Procesando pago con Mercado Pago. email={}, amount={}, installments={}, method={}, issuer={}, tokenPrefix={}",
                email,
                request.getTransactionAmount(),
                request.getInstallments(),
                request.getPaymentMethodId(),
                request.getIssuerId(),
                tokenPrefix);

        try {
            log.debug("Construyendo request para Mercado Pago. Validación previa completada. Token válido: {}", hasText(request.getToken()));
            
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

            log.debug("Enviando request a API Mercado Pago...");
            Payment payment = paymentClient.create(builder.build());
            log.info("Pago creado en Mercado Pago. id={}, status={}, statusDetail={}, metodo={}",
                    payment.getId(), payment.getStatus(), payment.getStatusDetail(), payment.getPaymentMethodId());
            return mapToResponse(payment);
        } catch (MPApiException mpEx) {
            // Intentar extraer información específica de la API de Mercado Pago
            String statusCode = "-";
            String responseBody = "";
            String requestId = "";
            try {
                Method mStatus = mpEx.getClass().getMethod("getStatusCode");
                Object s = mStatus.invoke(mpEx);
                statusCode = s != null ? String.valueOf(s) : "-";
            } catch (Exception ignore) {
                // método no disponible
            }
            try {
                // intentos comunes para obtener el body de la respuesta
                Method mBody = null;
                try { mBody = mpEx.getClass().getMethod("getResponseBody"); } catch (Exception e) { }
                if (mBody == null) {
                    try { mBody = mpEx.getClass().getMethod("getApiResponse"); } catch (Exception e) { }
                }
                if (mBody == null) {
                    try { mBody = mpEx.getClass().getMethod("getResponse"); } catch (Exception e) { }
                }
                if (mBody != null) {
                    Object body = mBody.invoke(mpEx);
                    responseBody = body != null ? String.valueOf(body) : "";
                }
            } catch (Exception ignore) {
                // no pudo extraer body
            }

            // intentar extraer request_id desde el body (si es JSON)
            if (responseBody != null && !responseBody.isEmpty()) {
                try {
                    Pattern p = Pattern.compile("\"request_id\"\\s*:\\s*\"([^\"]+)\"");
                    Matcher m = p.matcher(responseBody);
                    if (m.find()) {
                        requestId = m.group(1);
                    }
                } catch (Exception ignore) { }
            }

            String mpMessage = mpEx.getMessage() != null ? mpEx.getMessage() : "Sin mensaje de Mercado Pago";
            log.error("MercadoPago API error | status={} | request_id={} | body={} | message={} | method={} | issuer={} | amount={} | email={} | tokenPrefix={}",
                    statusCode,
                    requestId,
                    responseBody,
                    mpMessage,
                    request.getPaymentMethodId(),
                    request.getIssuerId(),
                    request.getTransactionAmount(),
                    email,
                    tokenPrefix);

            String respuestaAlUsuario = extraerMensajeError(responseBody != null && !responseBody.isEmpty() ? responseBody : mpMessage, mpEx.getClass().getSimpleName());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, respuestaAlUsuario);
        } catch (Exception ex) {
            String exceptionType = ex.getClass().getSimpleName();
            String exceptionMessage = ex.getMessage() != null ? ex.getMessage() : "Sin mensaje de error";
            log.error("❌ ERROR procesando pago. email={}, amount={}, method={}, issuer={}, tokenPrefix={}, exceptionType={}, error={}",
                    email,
                    request.getTransactionAmount(),
                    request.getPaymentMethodId(),
                    request.getIssuerId(),
                    tokenPrefix,
                    exceptionType,
                    exceptionMessage, ex);
            
            // Intentar extraer información útil del error
            String respuestaAlUsuario = extraerMensajeError(exceptionMessage, exceptionType);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, respuestaAlUsuario);
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

    /**
     * Intenta extraer un mensaje de error significativo de la excepción.
     * Mapea errores comunes de Mercado Pago a mensajes útiles sin exponer datos sensibles.
     */
    private String extraerMensajeError(String exceptionMessage, String exceptionType) {
        if (exceptionMessage == null) {
            exceptionMessage = "";
        }
        
        // Errores comunes de validación
        if (exceptionMessage.toLowerCase().contains("invalid token")) {
            return "El token de la tarjeta es inválido o expiró";
        }
        if (exceptionMessage.toLowerCase().contains("invalid payer")) {
            return "Los datos del pagador (email) son inválidos";
        }
        if (exceptionMessage.toLowerCase().contains("invalid payment method")) {
            return "El método de pago no es válido";
        }
        if (exceptionMessage.toLowerCase().contains("insufficient")) {
            return "Fondos insuficientes";
        }
        if (exceptionMessage.toLowerCase().contains("card declined")) {
            return "La tarjeta fue rechazada";
        }
        if (exceptionMessage.toLowerCase().contains("unauthorized") || 
            exceptionMessage.toLowerCase().contains("invalid credentials") ||
            exceptionMessage.toLowerCase().contains("access denied")) {
            return "Error de autenticación con Mercado Pago (credenciales inválidas o expiradas)";
        }
        if (exceptionMessage.toLowerCase().contains("timeout")) {
            return "Timeout: Mercado Pago tardó demasiado en responder. Intenta nuevamente.";
        }
        if (exceptionMessage.toLowerCase().contains("connection")) {
            return "Error de conexión con Mercado Pago. Intenta nuevamente.";
        }
        
        // Si no coincide con ningún patrón conocido
        return "No se pudo procesar el pago con Mercado Pago. Intenta nuevamente o contacta a soporte.";
    }
}


