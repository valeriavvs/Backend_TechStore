package com.upc.backend_techstore.controllers;

import com.upc.backend_techstore.dto.ProductoDto;
import com.upc.backend_techstore.interfaces.IProductoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@CrossOrigin(origins = "http://localhost:4200",
        allowCredentials = "true",
        exposedHeaders = "Authorization")
@RequestMapping("/api")

public class ProductoController {

    @Autowired
    private IProductoService productoService;

    // Registrar
    @PostMapping("/producto")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductoDto> registrarProducto(@RequestBody ProductoDto productoDto) {
        return ResponseEntity.ok(productoService.insertar(productoDto));
    }

    //Listar
    @GetMapping("/productos")
    public List<ProductoDto> listarProductos() {
        log.info("Lista de productos");
        return productoService.listar();
    }

    // Listar productos disponibles
    @GetMapping("/productos/activos")
    public ResponseEntity<List<ProductoDto>> listarProductosActivos() {
        return ResponseEntity.ok(productoService.listarActivos());
    }

    // Actualizar
    @PutMapping("/producto/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductoDto> actualizarProducto(@PathVariable Long id,
                                                          @RequestBody ProductoDto productoDto) throws Exception {
        return ResponseEntity.ok(productoService.actualizar(id, productoDto));
    }

    // Eliminar
    @DeleteMapping("/producto/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> eliminarProducto(@PathVariable Long id) throws Exception {
        productoService.eliminar(id);
        return ResponseEntity.ok("Producto eliminado correctamente");
    }

    //Disminuir stock
    @PutMapping("/producto/{id}/disminuir-stock")
    public ResponseEntity<ProductoDto> disminuirStock(@PathVariable Long id) {
        return ResponseEntity.ok(productoService.disminuirStock(id));
    }

    @PutMapping("/producto/{id}/disminuir-stock/{cantidad}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductoDto> disminuirStock(@PathVariable Long id, @PathVariable Integer cantidad) {
        return ResponseEntity.ok(productoService.disminuirStock(id, cantidad));
    }
}
