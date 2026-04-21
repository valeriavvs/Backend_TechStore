package com.upc.backend_techstore.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CarritoItemDto implements Serializable {
    private Long id;
    private Long idCarrito;
    private Long idProducto;
    private Integer cantidad;
    private String nombreProducto;
    private Double precio;
    private String imagen;
    private Double subtotal;
}
