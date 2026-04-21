package com.upc.backend_techstore.controllers;

import com.upc.backend_techstore.dto.CarritoItemDto;
import com.upc.backend_techstore.interfaces.ICarritoItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class CarritoItemController {

    @Autowired
    private ICarritoItemService carritoItemService;

    @PostMapping("/carrito-item")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public ResponseEntity<CarritoItemDto> guardar(@RequestBody CarritoItemDto carritoItemDto) throws Exception {
        return ResponseEntity.ok(carritoItemService.guardar(carritoItemDto));
    }

    @GetMapping("/carrito-items")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<CarritoItemDto>> listar() {
        return ResponseEntity.ok(carritoItemService.listar());
    }

    @GetMapping("/carrito-items/carrito/{idCarrito}")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public ResponseEntity<List<CarritoItemDto>> listarPorCarrito(@PathVariable Long idCarrito) {
        return ResponseEntity.ok(carritoItemService.listarPorCarrito(idCarrito));
    }

    @PutMapping("/carrito-item/{id}")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public ResponseEntity<CarritoItemDto> actualizar(@PathVariable Long id, @RequestBody CarritoItemDto carritoItemDto) throws Exception {
        return ResponseEntity.ok(carritoItemService.actualizar(id, carritoItemDto));
    }

    @DeleteMapping("/carrito-item/{id}")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public ResponseEntity<String> eliminar(@PathVariable Long id) throws Exception {
        carritoItemService.eliminar(id);
        return ResponseEntity.ok("Item de carrito eliminado correctamente");
    }
}