package com.upc.backend_techstore.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CarritoDto implements Serializable {
    private Long id;
    private Long idUsuario;
    private List<CarritoItemDto> items;
    private Double total;
}