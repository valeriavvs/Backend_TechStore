package com.upc.backend_techstore.services;

import com.upc.backend_techstore.Repository.PedidoRepository;
import com.upc.backend_techstore.Repository.ProductoRepository;
import com.upc.backend_techstore.Repository.UsuarioRepository;
import com.upc.backend_techstore.dto.DetallePedidoDto;
import com.upc.backend_techstore.dto.PedidoDto;
import com.upc.backend_techstore.entity.DetallePedido;
import com.upc.backend_techstore.entity.Pedido;
import com.upc.backend_techstore.entity.Producto;
import com.upc.backend_techstore.entity.Usuario;
import com.upc.backend_techstore.excepciones.InsufficientStockException;
import com.upc.backend_techstore.interfaces.IPedidoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PedidoService implements IPedidoService {

    @Autowired
    private PedidoRepository pedidoRepository;

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Override
    @Transactional
    public PedidoDto guardar(PedidoDto pedidoDto) {
        if (pedidoDto.getUsuarioId() == null) {
            throw new IllegalArgumentException("El pedido debe tener usuarioId");
        }

        if (pedidoDto.getDetalles() == null || pedidoDto.getDetalles().isEmpty()) {
            throw new IllegalArgumentException("El pedido debe tener al menos un detalle");
        }

        Usuario usuario = usuarioRepository.findById(pedidoDto.getUsuarioId())
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        Pedido pedido = new Pedido();
        pedido.setUsuario(usuario);
        pedido.setFecha(LocalDateTime.now());
        pedido.setEstado("Pendiente");
        pedido.setNombreCliente(pedidoDto.getNombreCliente());
        pedido.setDireccion(pedidoDto.getDireccion());
        pedido.setCelular(pedidoDto.getCelular());

        List<DetallePedido> detalles = new ArrayList<>();
        double total = 0.0;

        for (DetallePedidoDto detalleDto : pedidoDto.getDetalles()) {
            Producto producto = productoRepository.findByIdAndActivoTrue(detalleDto.getProductoId())
                    .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado"));

            if (producto.getStock() < detalleDto.getCantidad()) {
                throw new InsufficientStockException("Stock no disponible para el producto: " + producto.getNombre());
            }

            double precioUnitario = producto.getPrecio();
            double subTotal = precioUnitario * detalleDto.getCantidad();

            DetallePedido detalle = new DetallePedido();
            detalle.setCantidad(detalleDto.getCantidad());
            detalle.setPrecioUnitario(precioUnitario);
            detalle.setSubTotal(subTotal);
            detalle.setNombreProducto(producto.getNombre());
            detalle.setProducto(producto);
            detalle.setPedido(pedido);

            detalles.add(detalle);
            total += subTotal;

            producto.setStock(producto.getStock() - detalleDto.getCantidad());
            productoRepository.save(producto);
        }

        pedido.setDetalles(detalles);
        pedido.setTotal(total);

        Pedido guardado = pedidoRepository.save(pedido);
        return convertirPedidoADto(guardado);
    }

    @Override
    public List<PedidoDto> listar() {
        return pedidoRepository.findAll()
                .stream()
                .map(this::convertirPedidoADto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public PedidoDto actualizarEstado(Long id, String estado) throws Exception {
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new Exception("Pedido no encontrado"));

        String estadoNormalizado = estado == null ? "" : estado.trim().toLowerCase();
        String estadoFinal;
        String estadoActual = pedido.getEstado() == null ? "" : pedido.getEstado().trim().toLowerCase();

        switch (estadoNormalizado) {
            case "confirmado":
                estadoFinal = "Confirmado";
                break;
            case "cancelado":
                estadoFinal = "Cancelado";
                break;
            case "entregado":
                estadoFinal = "Entregado";
                break;
            default:
                throw new IllegalArgumentException("Estado no valido. Usa: Confirmado, Entregado o Cancelado");
        }

        if ("cancelado".equals(estadoActual) && !"cancelado".equals(estadoNormalizado)) {
            throw new IllegalArgumentException("El pedido ya esta cancelado y no puede cambiar de estado");
        }

        if ("cancelado".equals(estadoNormalizado) && !"cancelado".equals(estadoActual)) {
            for (DetallePedido detalle : pedido.getDetalles()) {
                Producto producto = productoRepository.findById(detalle.getProducto().getId())
                        .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado"));

                int stockActual = producto.getStock() == null ? 0 : producto.getStock();
                int cantidadADevolver = detalle.getCantidad() == null ? 0 : detalle.getCantidad();
                producto.setStock(stockActual + cantidadADevolver);
                productoRepository.save(producto);
            }
        }

        pedido.setEstado(estadoFinal);

        Pedido actualizado = pedidoRepository.save(pedido);
        return convertirPedidoADto(actualizado);
    }

    @Override
    public List<PedidoDto> listarPorUsarioId(Long idUsario) {
        return pedidoRepository.findByUsuarioId(idUsario)
                .stream()
                .map(this::convertirPedidoADto)
                .collect(Collectors.toList());
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

        double subTotal = detalle.getPrecioUnitario() * detalle.getCantidad();
        dto.setSubTotal(subTotal);

        dto.setNombreProducto(detalle.getNombreProducto());
        return dto;
    }
}
