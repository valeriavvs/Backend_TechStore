package com.upc.backend_techstore.services;

import com.upc.backend_techstore.Repository.CarritoItemRepository;
import com.upc.backend_techstore.Repository.CarritoRepository;
import com.upc.backend_techstore.Repository.ProductoRepository;
import com.upc.backend_techstore.dto.CarritoItemDto;
import com.upc.backend_techstore.entity.Carrito;
import com.upc.backend_techstore.entity.CarritoItem;
import com.upc.backend_techstore.entity.Producto;
import com.upc.backend_techstore.interfaces.ICarritoItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CarritoItemService implements ICarritoItemService {

    @Autowired
    private CarritoItemRepository carritoItemRepository;

    @Autowired
    private CarritoRepository carritoRepository;

    @Autowired
    private ProductoRepository productoRepository;

    @Override
    public CarritoItemDto guardar(CarritoItemDto carritoItemDto) throws Exception {
        Carrito carrito = carritoRepository.findById(carritoItemDto.getIdCarrito())
                .orElseThrow(() -> new Exception("Carrito no encontrado"));

        Producto producto = productoRepository.findByIdAndActivoTrue(carritoItemDto.getIdProducto())
                .orElseThrow(() -> new Exception("Producto no encontrado"));

        CarritoItem itemExistente = carritoItemRepository
                .findByCarritoIdAndProductoId(carritoItemDto.getIdCarrito(), carritoItemDto.getIdProducto())
                .orElse(null);

        if (itemExistente != null) {
            itemExistente.setCantidad(itemExistente.getCantidad() + carritoItemDto.getCantidad());
            itemExistente.setNombreProducto(producto.getNombre());
            itemExistente.setPrecio(producto.getPrecio());
            itemExistente.setSubtotal(itemExistente.getCantidad() * producto.getPrecio());
            itemExistente.setImagen(producto.getImagen());

            CarritoItem actualizado = carritoItemRepository.save(itemExistente);

            CarritoItemDto dto = new CarritoItemDto();
            dto.setId(actualizado.getId());
            dto.setIdCarrito(actualizado.getCarrito().getId());
            dto.setIdProducto(actualizado.getProducto().getId());
            dto.setCantidad(actualizado.getCantidad());
            dto.setNombreProducto(actualizado.getNombreProducto());
            dto.setPrecio(actualizado.getPrecio());
            dto.setSubtotal(actualizado.getSubtotal());
            dto.setImagen(actualizado.getImagen());
            return dto;
        }

        CarritoItem carritoItem = new CarritoItem();
        carritoItem.setCarrito(carrito);
        carritoItem.setProducto(producto);
        carritoItem.setCantidad(carritoItemDto.getCantidad());
        carritoItem.setNombreProducto(producto.getNombre());
        carritoItem.setPrecio(producto.getPrecio());
        carritoItem.setSubtotal(carritoItemDto.getCantidad() * producto.getPrecio());
        carritoItem.setImagen(producto.getImagen());

        CarritoItem guardado = carritoItemRepository.save(carritoItem);

        CarritoItemDto dto = new CarritoItemDto();
        dto.setId(guardado.getId());
        dto.setIdCarrito(guardado.getCarrito().getId());
        dto.setIdProducto(guardado.getProducto().getId());
        dto.setCantidad(guardado.getCantidad());
        dto.setNombreProducto(guardado.getNombreProducto());
        dto.setPrecio(guardado.getPrecio());
        dto.setSubtotal(guardado.getSubtotal());
        dto.setImagen(guardado.getImagen());

        return dto;
    }

    @Override
    public List<CarritoItemDto> listar() {
        return carritoItemRepository.findAll().stream().map(item -> {
            CarritoItemDto dto = new CarritoItemDto();
            dto.setId(item.getId());
            dto.setIdCarrito(item.getCarrito().getId());
            dto.setIdProducto(item.getProducto().getId());
            dto.setCantidad(item.getCantidad());
            dto.setNombreProducto(item.getNombreProducto());
            dto.setPrecio(item.getPrecio());
            dto.setSubtotal(item.getSubtotal());
            dto.setImagen(item.getImagen());
            return dto;
        }).collect(Collectors.toList());
    }

    @Override
    public List<CarritoItemDto> listarPorCarrito(Long idCarrito) {
        return carritoItemRepository.findByCarritoId(idCarrito).stream().map(item -> {
            CarritoItemDto dto = new CarritoItemDto();
            dto.setId(item.getId());
            dto.setIdCarrito(item.getCarrito().getId());
            dto.setIdProducto(item.getProducto().getId());
            dto.setCantidad(item.getCantidad());
            dto.setNombreProducto(item.getNombreProducto());
            dto.setPrecio(item.getPrecio());
            dto.setSubtotal(item.getSubtotal());
            dto.setImagen(item.getImagen());
            return dto;
        }).collect(Collectors.toList());
    }

    @Override
    public CarritoItemDto actualizar(Long id, CarritoItemDto carritoItemDto) throws Exception {
        CarritoItem item = carritoItemRepository.findById(id)
                .orElseThrow(() -> new Exception("Item de carrito no encontrado"));

        Carrito carrito = carritoRepository.findById(carritoItemDto.getIdCarrito())
                .orElseThrow(() -> new Exception("Carrito no encontrado"));

        Producto producto = productoRepository.findByIdAndActivoTrue(carritoItemDto.getIdProducto())
                .orElseThrow(() -> new Exception("Producto no encontrado"));

        item.setCarrito(carrito);
        item.setProducto(producto);
        item.setCantidad(carritoItemDto.getCantidad());
        item.setNombreProducto(producto.getNombre());
        item.setPrecio(producto.getPrecio());
        item.setSubtotal(item.getCantidad() * producto.getPrecio());
        item.setImagen(producto.getImagen());

        CarritoItem actualizado = carritoItemRepository.save(item);

        CarritoItemDto dto = new CarritoItemDto();
        dto.setId(actualizado.getId());
        dto.setIdCarrito(actualizado.getCarrito().getId());
        dto.setIdProducto(actualizado.getProducto().getId());
        dto.setCantidad(actualizado.getCantidad());
        dto.setNombreProducto(actualizado.getNombreProducto());
        dto.setPrecio(actualizado.getPrecio());
        dto.setSubtotal(actualizado.getSubtotal());
        dto.setImagen(actualizado.getImagen());

        return dto;
    }

    @Override
    public void eliminar(Long id) throws Exception {
        CarritoItem item = carritoItemRepository.findById(id)
                .orElseThrow(() -> new Exception("Item de carrito no encontrado"));

        carritoItemRepository.delete(item);
    }
}