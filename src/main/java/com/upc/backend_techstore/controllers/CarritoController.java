package com.upc.backend_techstore.controllers;

import com.upc.backend_techstore.dto.CarritoDto;
import com.upc.backend_techstore.dto.PedidoDto;
import com.upc.backend_techstore.interfaces.ICarritoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class CarritoController {

    @Autowired
    private ICarritoService carritoService;

    @PostMapping("/carrito")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public ResponseEntity<CarritoDto> guardar(@RequestBody CarritoDto carritoDto) throws Exception {
        return ResponseEntity.ok(carritoService.guardar(carritoDto));
    }

    @GetMapping("/carritos")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<CarritoDto>> listar() {
        return ResponseEntity.ok(carritoService.listar());
    }

    @GetMapping("/carrito/{id}")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public ResponseEntity<CarritoDto> buscarPorId(@PathVariable Long id) throws Exception {
        return ResponseEntity.ok(carritoService.buscarPorId(id));
    }

    @GetMapping("/carrito/usuario/{idUsuario}")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public ResponseEntity<CarritoDto> buscarPorUsuario(@PathVariable Long idUsuario) throws Exception {
        return ResponseEntity.ok(carritoService.buscarPorUsuario(idUsuario));
    }

    @PutMapping("/carrito/{id}")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public ResponseEntity<CarritoDto> actualizar(@PathVariable Long id, @RequestBody CarritoDto carritoDto) throws Exception {
        return ResponseEntity.ok(carritoService.actualizar(id, carritoDto));
    }

    @PostMapping("/carrito/{idCarrito}/confirmar")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public ResponseEntity<PedidoDto> confirmarCarrito(@PathVariable Long idCarrito, Authentication authentication) {
        return ResponseEntity.ok(carritoService.confirmarCarrito(idCarrito, authentication.getName()));
    }

    @DeleteMapping("/carrito/{id}")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public ResponseEntity<String> eliminar(@PathVariable Long id) throws Exception {
        carritoService.eliminar(id);
        return ResponseEntity.ok("Carrito eliminado correctamente");
    }
}