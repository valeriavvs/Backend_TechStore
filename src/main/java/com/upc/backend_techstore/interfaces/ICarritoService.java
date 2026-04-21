package com.upc.backend_techstore.interfaces;

import com.upc.backend_techstore.dto.CarritoDto;
import com.upc.backend_techstore.dto.PedidoDto;

import java.util.List;

public interface ICarritoService {
    CarritoDto guardar(CarritoDto carritoDto) throws Exception;
    List<CarritoDto> listar();
    CarritoDto actualizar(Long id, CarritoDto carritoDto) throws Exception;
    void eliminar(Long id) throws Exception;
    CarritoDto buscarPorId(Long id) throws Exception;
    CarritoDto buscarPorUsuario(Long idUsuario) throws Exception;
    PedidoDto confirmarCarrito(Long idCarrito, String emailAutenticado);
}
