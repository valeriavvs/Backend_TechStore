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
public class ProductoDto implements Serializable {
    private Long id;
    private String nombre;
    private String descripcion;
    private double precio;
    private Integer stock;
    private String imagenUrl;


    private Boolean activo;
}
