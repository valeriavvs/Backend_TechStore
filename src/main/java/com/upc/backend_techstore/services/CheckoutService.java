package com.upc.backend_techstore.services;

import com.upc.backend_techstore.dto.CheckoutRequestDto;
import com.upc.backend_techstore.dto.CheckoutResponseDto;
import com.upc.backend_techstore.dto.PaymentRequestDto;
import com.upc.backend_techstore.dto.PaymentResponseDto;
import com.upc.backend_techstore.dto.PedidoDto;
import com.upc.backend_techstore.interfaces.ICarritoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
public class CheckoutService {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private ICarritoService carritoService;

    @Transactional
    public CheckoutResponseDto confirmarCompra(CheckoutRequestDto request, String emailAutenticado) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El body del checkout es obligatorio");
        }

        if (request.getCarritoId() == null || request.getCarritoId() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El carritoId es obligatorio");
        }

        log.info("Iniciando checkout. carritoId={}, email={}", request.getCarritoId(), emailAutenticado);

        // Convertir CheckoutRequestDto a PaymentRequestDto
        PaymentRequestDto paymentRequest = new PaymentRequestDto();
        paymentRequest.setToken(request.getToken());
        paymentRequest.setIssuerId(request.getIssuerId());
        paymentRequest.setPaymentMethodId(request.getPaymentMethodId());
        paymentRequest.setTransactionAmount(request.getTransactionAmount());
        paymentRequest.setInstallments(request.getInstallments());
        paymentRequest.setDescription(request.getDescription());

        PaymentRequestDto.PayerDto payer = new PaymentRequestDto.PayerDto();
        payer.setEmail(request.getPayer() != null ? request.getPayer().getEmail() : emailAutenticado);
        paymentRequest.setPayer(payer);

        // Procesar pago con Mercado Pago EN SU PROPIA TRANSACCIÓN
        // Si el pago se aprueba, la transacción se confirma independientemente de lo que pase después
        PaymentResponseDto paymentResponse = procesarPagoEnTransaccionAislada(paymentRequest, request.getCarritoId());

        // Solo crear pedido si el pago fue approved
        if (!"approved".equalsIgnoreCase(paymentResponse.getStatus())) {
            log.warn("Pago no aprobado. status={}, carritoId={}", paymentResponse.getStatus(), request.getCarritoId());
            throw new ResponseStatusException(
                    HttpStatus.PAYMENT_REQUIRED,
                    "Pago rechazado o pendiente. Status: " + paymentResponse.getStatus() + 
                    ". Detalle: " + paymentResponse.getStatusDetail()
            );
        }

        // Confirmar carrito y crear pedido (EN ESTA TRANSACCIÓN)
        PedidoDto pedidoDto;
        try {
            pedidoDto = carritoService.confirmarCarrito(request.getCarritoId(), emailAutenticado);
            log.info("Pedido creado exitosamente. pedidoId={}, carritoId={}", pedidoDto.getId(), request.getCarritoId());
        } catch (Exception ex) {
            log.error("⚠️ INCIDENCIA CRÍTICA: Pago aprobado en Mercado Pago (ID: {}), pero FALLO la creación del pedido. carritoId={}, error={}, email={}. REVISIÓN MANUAL REQUERIDA.",
                    paymentResponse.getIdPago(), request.getCarritoId(), ex.getMessage(), emailAutenticado);
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "El pago fue aprobado en Mercado Pago, pero hubo un error al crear el pedido. Por favor contacta a soporte. ID de pago: " + paymentResponse.getIdPago()
            );
        }

        // Construir respuesta unificada
        CheckoutResponseDto response = new CheckoutResponseDto();
        response.setIdPago(paymentResponse.getIdPago());
        response.setPaymentStatus(paymentResponse.getStatus());
        response.setPaymentStatusDetail(paymentResponse.getStatusDetail());
        response.setPaymentAmount(paymentResponse.getMonto());
        response.setPaymentDate(paymentResponse.getFecha());
        response.setPaymentMethod(paymentResponse.getMetodoPago());
        response.setPedido(pedidoDto);

        log.info("Checkout completado exitosamente. pedidoId={}, paymentId={}", pedidoDto.getId(), paymentResponse.getIdPago());
        return response;
    }

    /**
     * Procesa el pago en su PROPIA TRANSACCIÓN aislada (REQUIRES_NEW).
     * Esto garantiza que si el pago se aprueba, el cambio se persiste en BD incluso
     * si la transacción principal falla después (en creación de pedido).
     * 
     * De esta forma, evitamos inconsistencias donde Mercado Pago tiene el pago aprobado
     * pero nuestra BD hace rollback del pedido.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    private PaymentResponseDto procesarPagoEnTransaccionAislada(PaymentRequestDto paymentRequest, Long carritoId) {
        try {
            PaymentResponseDto paymentResponse = paymentService.processCardPayment(paymentRequest);
            log.info("Pago procesado en transacción aislada. status={}, carritoId={}, paymentId={}", 
                    paymentResponse.getStatus(), carritoId, paymentResponse.getIdPago());
            return paymentResponse;
        } catch (Exception ex) {
            log.error("Error al procesar pago en transacción aislada. carritoId={}, error={}", carritoId, ex.getMessage());
            throw ex;
        }
    }
}



