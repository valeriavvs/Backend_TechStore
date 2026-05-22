package com.upc.backend_techstore.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CheckoutResponseDto implements Serializable {
    // Datos del pago
    private Long idPago;
    private String paymentStatus;      // approved, rejected, pending
    private String paymentStatusDetail;
    private BigDecimal paymentAmount;
    private OffsetDateTime paymentDate;
    private String paymentMethod;

    // Datos del pedido creado
    private PedidoDto pedido;
}

