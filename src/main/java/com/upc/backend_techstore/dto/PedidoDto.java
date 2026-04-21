package com.upc.backend_techstore.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PedidoDto implements Serializable {
    private Long id;
    private Long usuarioId;
    private LocalDateTime fecha;
    private String estado;
    private Double total;
    private String nombreCliente;
    private String direccion;
    private String celular;
    private List<DetallePedidoDto> detalles;
}