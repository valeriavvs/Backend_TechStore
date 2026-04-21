package com.upc.backend_techstore.controllers;

import com.upc.backend_techstore.dto.UsuarioDto;
import com.upc.backend_techstore.services.UsuarioService;
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

public class UsuarioController {
    @Autowired
    private UsuarioService usuarioService;

    //Listar
    @GetMapping("/usuarios")
    @PreAuthorize("hasRole('ADMIN')")
    public List<UsuarioDto> UsuarioCategorias(){
        log.info("Lista de usuario");
        return usuarioService.listar();
    }

    //Registrar
    @PostMapping("/auth/usuario")
    public ResponseEntity<UsuarioDto> registrarUsuario(@RequestBody UsuarioDto usuario) {
        log.info("Registrando usuario {}", usuario.getEmail());
        return ResponseEntity.ok(usuarioService.insertar(usuario));
    }

    // Actualizar
    @PutMapping("/usuario/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UsuarioDto> actualizarUsuario(@PathVariable Long id, @RequestBody UsuarioDto usuario) throws Exception {
        log.info("Actualizando usuario con id {}", id);
        return ResponseEntity.ok(usuarioService.actualizar(id, usuario));
    }

    // Eliminar
    @DeleteMapping("/usuario/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> eliminarUsuario(@PathVariable Long id) throws Exception {
        log.info("Eliminando usuario con id {}", id);
        usuarioService.eliminar(id);
        return ResponseEntity.ok("Usuario eliminada correctamente");
    }
}
