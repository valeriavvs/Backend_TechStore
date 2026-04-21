package com.upc.backend_techstore.controllers;

import com.upc.backend_techstore.dto.PedidoDto;
import com.upc.backend_techstore.interfaces.IPedidoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

        import java.util.List;

@RestController
@RequestMapping("/api")
public class PedidoController {

    @Autowired
    private IPedidoService pedidoService;

    // Registrar pedido
    @PostMapping("/pedido")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public ResponseEntity<PedidoDto> registrarPedido(@RequestBody PedidoDto pedidoDto) {
        return ResponseEntity.ok(pedidoService.guardar(pedidoDto));
    }

    // Listar pedidos
    @GetMapping("/pedidos")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<PedidoDto>> listarPedidos() {
        return ResponseEntity.ok(pedidoService.listar());
    }

    // Actualizar estado del pedido
    @PutMapping("/pedido/{id}/estado")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PedidoDto> actualizarEstado(@PathVariable Long id, @RequestParam String estado) throws Exception {
        return ResponseEntity.ok(pedidoService.actualizarEstado(id, estado));
    }

    //Listar pedidos por id
    @GetMapping("/mis-pedidos/usuario/{id}")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public ResponseEntity<List<PedidoDto>> listarMisPedidos(@PathVariable Long id) {
        return ResponseEntity.ok(pedidoService.listarPorUsarioId(id));
    }
}