package com.upc.backend_techstore.services;

import com.upc.backend_techstore.Repository.DetallePedidoRepository;
import com.upc.backend_techstore.Repository.PedidoRepository;
import com.upc.backend_techstore.Repository.ProductoRepository;
import com.upc.backend_techstore.dto.DetallePedidoDto;
import com.upc.backend_techstore.entity.DetallePedido;
import com.upc.backend_techstore.entity.Pedido;
import com.upc.backend_techstore.entity.Producto;
import com.upc.backend_techstore.interfaces.IDetallePedidoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class DetallePedidoService implements IDetallePedidoService {

    @Autowired
    private DetallePedidoRepository detallePedidoRepository;

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private PedidoRepository pedidoRepository;

    @Override
    public DetallePedidoDto guardar(DetallePedidoDto detallePedidoDto) throws Exception {
        Producto producto = productoRepository.findById(detallePedidoDto.getProductoId())
                .orElseThrow(() -> new Exception("Producto no encontrado"));

        Pedido pedido = pedidoRepository.findById(detallePedidoDto.getPedidoId())
                .orElseThrow(() -> new Exception("Pedido no encontrado"));

        DetallePedido detalle = new DetallePedido();
        detalle.setCantidad(detallePedidoDto.getCantidad());

        double precioUnitario = producto.getPrecio();
        double subTotal = precioUnitario * detallePedidoDto.getCantidad();

        detalle.setPrecioUnitario(precioUnitario);
        detalle.setSubTotal(subTotal);
        detalle.setProducto(producto);
        detalle.setPedido(pedido);
        detalle.setNombreProducto(producto.getNombre());

        DetallePedido guardado = detallePedidoRepository.save(detalle);

        return convertirADto(guardado);
    }

    @Override
    public List<DetallePedidoDto> listar() {
        return detallePedidoRepository.findAll()
                .stream()
                .map(this::convertirADto)
                .collect(Collectors.toList());
    }

    @Override
    public DetallePedidoDto actualizar(Long id, DetallePedidoDto detallePedidoDto) throws Exception {
        DetallePedido detalle = detallePedidoRepository.findById(id)
                .orElseThrow(() -> new Exception("Detalle no encontrado"));

        Producto producto = productoRepository.findById(detallePedidoDto.getProductoId())
                .orElseThrow(() -> new Exception("Producto no encontrado"));

        Pedido pedido = pedidoRepository.findById(detallePedidoDto.getPedidoId())
                .orElseThrow(() -> new Exception("Pedido no encontrado"));

        detalle.setCantidad(detallePedidoDto.getCantidad());

        double precioUnitario = producto.getPrecio();
        double subTotal = precioUnitario * detallePedidoDto.getCantidad();

        detalle.setPrecioUnitario(precioUnitario);
        detalle.setSubTotal(subTotal);
        detalle.setProducto(producto);
        detalle.setPedido(pedido);
        detalle.setNombreProducto(producto.getNombre());

        DetallePedido actualizado = detallePedidoRepository.save(detalle);

        return convertirADto(actualizado);
    }

    @Override
    public void eliminar(Long id) throws Exception {
        DetallePedido detalle = detallePedidoRepository.findById(id)
                .orElseThrow(() -> new Exception("Detalle no encontrado"));

        detallePedidoRepository.delete(detalle);
    }

    private DetallePedidoDto convertirADto(DetallePedido detalle) {
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