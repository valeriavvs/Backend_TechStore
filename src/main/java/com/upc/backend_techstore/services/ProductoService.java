package com.upc.backend_techstore.services;

import com.upc.backend_techstore.Repository.ProductoRepository;
import com.upc.backend_techstore.dto.ProductoDto;
import com.upc.backend_techstore.entity.Producto;
import com.upc.backend_techstore.excepciones.InsufficientStockException;
import com.upc.backend_techstore.interfaces.IProductoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProductoService implements IProductoService {

    @Autowired
    private ProductoRepository productoRepository;

    //Listar
    @Override
    public List<ProductoDto> listar() {
        return productoRepository.findAll()
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    //Listar activos
    @Override
    public List<ProductoDto> listarActivos() {
        return productoRepository.findByActivoTrue()
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    //INSERTAR
    @Override
    public ProductoDto insertar(ProductoDto productoDto) {
        Producto productoEntity = toEntity(productoDto);
        Producto guardado = productoRepository.save(productoEntity);
        return toDto(guardado);
    }

    //ACTUALIZAR
    @Override
    public ProductoDto actualizar(Long id, ProductoDto productoDto) throws Exception {
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new Exception("Producto no encontrado"));

        // Actualiza campos solo si no son null (evita sobrescribir con null)
        if (productoDto.getNombre() != null) {
            producto.setNombre(productoDto.getNombre());
        }
        if (productoDto.getDescripcion() != null) {
            producto.setDescripcion(productoDto.getDescripcion());
        }
        if (productoDto.getPrecio() > 0) {
            producto.setPrecio(productoDto.getPrecio());
        }
        if (productoDto.getStock() != null) {
            producto.setStock(productoDto.getStock());
        }
        if (productoDto.getImagenUrl() != null) {
            producto.setImagen(productoDto.getImagenUrl());
        }
        if (productoDto.getActivo() != null) {
            producto.setActivo(productoDto.getActivo());
        }

        Producto actualizado = productoRepository.save(producto);
        return toDto(actualizado);
    }


    //ELIMINAR
    @Override
    public void eliminar(Long id) throws Exception {
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new Exception("Producto no encontrado"));

        // Eliminación lógica: desactivar producto en lugar de borrarlo
        producto.setActivo(false);
        productoRepository.save(producto);
    }

    @Override
    @Transactional
    public ProductoDto disminuirStock(Long id) {
        return disminuirStock(id, 1);
    }

    @Override
    @Transactional
    public ProductoDto disminuirStock(Long id, Integer cantidad) {
        if (id == null) {
            throw new IllegalArgumentException("El id del producto es obligatorio");
        }

        if (cantidad == null || cantidad <= 0) {
            throw new IllegalArgumentException("La cantidad a descontar debe ser mayor que cero");
        }

        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new java.util.NoSuchElementException("Producto no encontrado"));

        Integer stockActual = producto.getStock();
        if (stockActual == null) {
            throw new IllegalStateException("El producto no tiene stock configurado");
        }

        if (stockActual <= 0) {
            throw new InsufficientStockException(producto.getNombre(), cantidad, stockActual);
        }

        if (stockActual < cantidad) {
            throw new InsufficientStockException(producto.getNombre(), cantidad, stockActual);
        }

        producto.setStock(stockActual - cantidad);

        return toDto(productoRepository.save(producto));
    }

    private ProductoDto toDto(Producto producto) {
        ProductoDto dto = new ProductoDto();
        dto.setId(producto.getId());
        dto.setNombre(producto.getNombre());
        dto.setDescripcion(producto.getDescripcion());
        dto.setPrecio(producto.getPrecio());
        dto.setStock(producto.getStock());
        dto.setImagenUrl(producto.getImagen());
        dto.setActivo(producto.getActivo());
        return dto;
    }

    private Producto toEntity(ProductoDto productoDto) {
        Producto producto = new Producto();
        producto.setNombre(productoDto.getNombre());
        producto.setDescripcion(productoDto.getDescripcion());
        producto.setPrecio(productoDto.getPrecio());
        producto.setStock(productoDto.getStock());
        producto.setImagen(productoDto.getImagenUrl());
        producto.setActivo(productoDto.getActivo() != null ? productoDto.getActivo() : true);
        return producto;
    }
}
