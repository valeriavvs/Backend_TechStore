package com.upc.backend_techstore.services;

import com.upc.backend_techstore.Repository.ProductoRepository;
import com.upc.backend_techstore.dto.ProductoDto;
import com.upc.backend_techstore.entity.Producto;
import com.upc.backend_techstore.excepciones.InsufficientStockException;
import com.upc.backend_techstore.interfaces.IProductoService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProductoService implements IProductoService {

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private ModelMapper modelMapper;

    //Listar
    @Override
    public List<ProductoDto> listar() {
        return productoRepository.findAll()
                .stream()
                .map(p -> modelMapper.map(p, ProductoDto.class))
                .collect(Collectors.toList());
    }

    //Listar activos
    @Override
    public List<ProductoDto> listarActivos() {
        return productoRepository.findByActivoTrue()
                .stream()
                .map(p -> modelMapper.map(p, ProductoDto.class))
                .collect(Collectors.toList());
    }

    //INSERTAR
    @Override
    public ProductoDto insertar(ProductoDto productoDto) {
        Producto productoEntity = modelMapper.map(productoDto, Producto.class);
        Producto guardado = productoRepository.save(productoEntity);
        return modelMapper.map(guardado, ProductoDto.class);
    }

    //ACTUALIZAR
    @Override
    public ProductoDto actualizar(Long id, ProductoDto productoDto) throws Exception {
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new Exception("Producto no encontrado"));

        producto.setNombre(productoDto.getNombre());
        producto.setDescripcion(productoDto.getDescripcion());
        producto.setPrecio(productoDto.getPrecio());
        producto.setStock(productoDto.getStock());
        producto.setImagen(productoDto.getImagen());
        producto.setActivo(productoDto.getActivo());

        Producto actualizado = productoRepository.save(producto);
        return modelMapper.map(actualizado, ProductoDto.class);
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

        return modelMapper.map(productoRepository.save(producto), ProductoDto.class);
    }
}
