package com.upc.backend_techstore.interfaces;

import com.upc.backend_techstore.dto.ProductoDto;

import java.util.List;

public interface IProductoService {
    List<ProductoDto> listar();
    List<ProductoDto> listarActivos();
    ProductoDto insertar(ProductoDto producto);
    ProductoDto actualizar(Long id, ProductoDto productoDto) throws Exception;
    void eliminar(Long id) throws Exception;
    ProductoDto disminuirStock(Long id);
    ProductoDto disminuirStock(Long id, Integer cantidad);

}
