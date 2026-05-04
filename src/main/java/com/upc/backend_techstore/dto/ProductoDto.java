package com.upc.backend_techstore.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
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
    @JsonProperty(value = "imagen", access = JsonProperty.Access.READ_WRITE)
    private String imagen;
    @JsonProperty(value = "imagenUrl", access = JsonProperty.Access.READ_WRITE)
    private String imagenUrl;
    private Boolean activo;

    // Prioriza imagenUrl si viene del request (JSON), sino usa imagen
    public String getImagen() {
        return imagenUrl != null && !imagenUrl.isBlank() ? imagenUrl : imagen;
    }
}
