package com.upc.backend_techstore.services;

import com.upc.backend_techstore.Repository.CarritoItemRepository;
import com.upc.backend_techstore.Repository.CarritoRepository;
import com.upc.backend_techstore.Repository.PedidoRepository;
import com.upc.backend_techstore.Repository.ProductoRepository;
import com.upc.backend_techstore.Repository.UsuarioRepository;
import com.upc.backend_techstore.dto.CarritoDto;
import com.upc.backend_techstore.dto.CarritoItemDto;
import com.upc.backend_techstore.dto.DetallePedidoDto;
import com.upc.backend_techstore.dto.PedidoDto;
import com.upc.backend_techstore.entity.Carrito;
import com.upc.backend_techstore.entity.CarritoItem;
import com.upc.backend_techstore.entity.DetallePedido;
import com.upc.backend_techstore.entity.Pedido;
import com.upc.backend_techstore.entity.Producto;
import com.upc.backend_techstore.entity.Usuario;
import com.upc.backend_techstore.excepciones.InsufficientStockException;
import com.upc.backend_techstore.interfaces.ICarritoService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Service
public class CarritoService implements ICarritoService {

    @Autowired
    private CarritoRepository carritoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private CarritoItemRepository carritoItemRepository;

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private PedidoRepository pedidoRepository;

    @Override
    public CarritoDto guardar(CarritoDto carritoDto) throws Exception {
        Usuario usuario = usuarioRepository.findById(carritoDto.getIdUsuario())
                .orElseThrow(() -> new Exception("Usuario no encontrado"));

        if (carritoRepository.findByUsuarioId(carritoDto.getIdUsuario()).isPresent()) {
            throw new Exception("El usuario ya tiene un carrito");
        }

        Carrito carrito = new Carrito();
        carrito.setUsuario(usuario);

        Carrito guardado = carritoRepository.save(carrito);

        CarritoDto dto = new CarritoDto();
        dto.setId(guardado.getId());
        dto.setIdUsuario(guardado.getUsuario().getId());
        dto.setItems(List.of());
        dto.setTotal(0.0);

        return dto;
    }

    @Override
    public List<CarritoDto> listar() {
        return carritoRepository.findAll().stream().map(this::convertirADtoConItems)
                .collect(Collectors.toList());
    }

    @Override
    public CarritoDto actualizar(Long id, CarritoDto carritoDto) throws Exception {
        Carrito carrito = carritoRepository.findById(id)
                .orElseThrow(() -> new Exception("Carrito no encontrado"));

        Usuario usuario = usuarioRepository.findById(carritoDto.getIdUsuario())
                .orElseThrow(() -> new Exception("Usuario no encontrado"));

        carrito.setUsuario(usuario);

        Carrito actualizado = carritoRepository.save(carrito);

        return convertirADtoConItems(actualizado);
    }

    @Override
    public void eliminar(Long id) throws Exception {
        Carrito carrito = carritoRepository.findById(id)
                .orElseThrow(() -> new Exception("Carrito no encontrado"));

        carritoRepository.delete(carrito);
    }

    @Override
    public CarritoDto buscarPorId(Long id) throws Exception {
        Carrito carrito = carritoRepository.findById(id)
                .orElseThrow(() -> new Exception("Carrito no encontrado"));

        return convertirADtoConItems(carrito);
    }

    @Override
    public CarritoDto buscarPorUsuario(Long idUsuario) throws Exception {
        Carrito carrito = carritoRepository.findByUsuarioId(idUsuario)
                .orElseThrow(() -> new Exception("Carrito no encontrado para este usuario"));

        return convertirADtoConItems(carrito);
    }

    @Override
    @Transactional
    public PedidoDto confirmarCarrito(Long idCarrito, String emailAutenticado) {
        if (idCarrito == null) {
            throw new IllegalArgumentException("El id del carrito es obligatorio");
        }

        Usuario usuarioAutenticado = usuarioRepository.findByEmail(emailAutenticado)
                .orElseThrow(() -> new NoSuchElementException("Usuario autenticado no encontrado"));

        Carrito carrito = carritoRepository.findById(idCarrito)
                .orElseThrow(() -> new NoSuchElementException("Carrito no encontrado"));

        if (!carrito.getUsuario().getId().equals(usuarioAutenticado.getId())) {
            throw new AccessDeniedException("No tienes permisos para confirmar este carrito");
        }

        List<CarritoItem> items = carritoItemRepository.findByCarritoId(idCarrito);
        if (items.isEmpty()) {
            throw new IllegalArgumentException("El carrito esta vacio");
        }

        Map<Long, Producto> productosBloqueados = new HashMap<>();

        for (CarritoItem item : items) {
            if (item.getCantidad() == null || item.getCantidad() <= 0) {
                throw new IllegalArgumentException("Cantidad invalida para el producto " + item.getNombreProducto());
            }

            Producto producto = productoRepository.findByIdAndActivoTrueForUpdate(item.getProducto().getId())
                    .orElseThrow(() -> new NoSuchElementException("Producto no encontrado: " + item.getProducto().getId()));

            productosBloqueados.put(item.getProducto().getId(), producto);

            if (producto.getStock() == null || producto.getStock() < item.getCantidad()) {
                throw new InsufficientStockException(producto.getNombre(), item.getCantidad(), producto.getStock() == null ? 0 : producto.getStock());
            }
        }

        Pedido pedido = new Pedido();
        pedido.setUsuario(usuarioAutenticado);
        pedido.setFecha(LocalDateTime.now());
        pedido.setEstado("Pendiente");
        pedido.setNombreCliente(usuarioAutenticado.getNombre());
        pedido.setDireccion("No especificada");
        pedido.setCelular("No especificado");

        List<DetallePedido> detalles = new ArrayList<>();
        double total = 0.0;

        for (CarritoItem item : items) {
            Producto producto = productosBloqueados.get(item.getProducto().getId());

            producto.setStock(producto.getStock() - item.getCantidad());
            productoRepository.save(producto);

            DetallePedido detalle = new DetallePedido();
            detalle.setPedido(pedido);
            detalle.setProducto(producto);
            detalle.setCantidad(item.getCantidad());
            detalle.setPrecioUnitario(producto.getPrecio());
            detalle.setSubTotal(producto.getPrecio() * item.getCantidad());
            detalle.setNombreProducto(producto.getNombre());

            detalles.add(detalle);
            total += detalle.getSubTotal();
        }

        pedido.setDetalles(detalles);
        pedido.setTotal(total);

        Pedido pedidoGuardado = pedidoRepository.save(pedido);
        carritoItemRepository.deleteAll(items);

        return convertirPedidoADto(pedidoGuardado);
    }

    private CarritoDto convertirADtoConItems(Carrito carrito) {
        List<CarritoItemDto> items = carritoItemRepository.findByCarritoId(carrito.getId()).stream().map(item -> {
            CarritoItemDto dto = new CarritoItemDto();
            dto.setId(item.getId());
            dto.setIdCarrito(item.getCarrito().getId());
            dto.setIdProducto(item.getProducto().getId());
            dto.setCantidad(item.getCantidad());
            dto.setNombreProducto(item.getNombreProducto());
            dto.setPrecio(item.getPrecio());
            dto.setImagen(item.getImagen());
            dto.setSubtotal(item.getSubtotal());
            return dto;
        }).collect(Collectors.toList());

        double total = items.stream()
                .mapToDouble(CarritoItemDto::getSubtotal)
                .sum();

        CarritoDto dto = new CarritoDto();
        dto.setId(carrito.getId());
        dto.setIdUsuario(carrito.getUsuario().getId());
        dto.setItems(items);
        dto.setTotal(total);

        return dto;
    }

    private PedidoDto convertirPedidoADto(Pedido pedido) {
        PedidoDto dto = new PedidoDto();
        dto.setId(pedido.getId());
        dto.setUsuarioId(pedido.getUsuario().getId());
        dto.setFecha(pedido.getFecha());
        dto.setEstado(pedido.getEstado());
        dto.setTotal(pedido.getTotal());
        dto.setNombreCliente(pedido.getNombreCliente());
        dto.setDireccion(pedido.getDireccion());
        dto.setCelular(pedido.getCelular());

        List<DetallePedidoDto> detallesDto = pedido.getDetalles().stream()
                .map(this::convertirDetalleADto)
                .collect(Collectors.toList());

        dto.setDetalles(detallesDto);
        return dto;
    }

    private DetallePedidoDto convertirDetalleADto(DetallePedido detalle) {
        DetallePedidoDto dto = new DetallePedidoDto();
        dto.setId(detalle.getId());
        dto.setPedidoId(detalle.getPedido().getId());
        dto.setProductoId(detalle.getProducto().getId());
        dto.setCantidad(detalle.getCantidad());
        dto.setPrecioUnitario(detalle.getPrecioUnitario());
        dto.setSubTotal(detalle.getSubTotal());
        dto.setNombreProducto(detalle.getNombreProducto());
        return dto;
    }
}