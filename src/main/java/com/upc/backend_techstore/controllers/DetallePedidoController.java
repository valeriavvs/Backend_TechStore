package com.upc.backend_techstore.controllers;

import com.upc.backend_techstore.dto.DetallePedidoDto;
import com.upc.backend_techstore.interfaces.IDetallePedidoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class DetallePedidoController {

    @Autowired
    private IDetallePedidoService detallePedidoService;

    //Registrar un detalle de pedido
    @PostMapping("/detallePedido")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DetallePedidoDto> registrar(@RequestBody DetallePedidoDto detallePedidoDto) throws Exception {
        return ResponseEntity.ok(detallePedidoService.guardar(detallePedidoDto));
    }

    //Listar detalles de pedidos
    @GetMapping("/detallePedidos")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<DetallePedidoDto>> listar() {
        return ResponseEntity.ok(detallePedidoService.listar());
    }

    //Actualizar un detalle de pedido
    @PutMapping("/detallePedido/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DetallePedidoDto> actualizar(@PathVariable Long id,
                                                       @RequestBody DetallePedidoDto detallePedidoDto) throws Exception {
        return ResponseEntity.ok(detallePedidoService.actualizar(id, detallePedidoDto));
    }

    //Eliminar un detalle de pedido
    @DeleteMapping("/detallePedido/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> eliminar(@PathVariable Long id) throws Exception {
        detallePedidoService.eliminar(id);
        return ResponseEntity.ok("Detalle de pedido eliminado correctamente");
    }

}
