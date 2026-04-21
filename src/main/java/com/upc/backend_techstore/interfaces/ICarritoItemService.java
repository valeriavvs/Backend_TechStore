package com.upc.backend_techstore.interfaces;

import com.upc.backend_techstore.dto.CarritoItemDto;

import java.util.List;

public interface ICarritoItemService {
    CarritoItemDto guardar(CarritoItemDto carritoItemDto) throws Exception;
    List<CarritoItemDto> listar();
    List<CarritoItemDto> listarPorCarrito(Long idCarrito);
    CarritoItemDto actualizar(Long id, CarritoItemDto carritoItemDto) throws Exception;
    void eliminar(Long id) throws Exception;
}
