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
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponseDto implements Serializable {

    private Long idPago;
    private String status;
    private String statusDetail;
    private BigDecimal monto;
    private OffsetDateTime fecha;
    private String metodoPago;
}

