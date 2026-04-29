package com.upc.backend_techstore.security.controllers;

import com.upc.backend_techstore.Repository.UsuarioRepository;
import com.upc.backend_techstore.entity.Usuario;
import com.upc.backend_techstore.security.dtos.AuthRequestDTO;
import com.upc.backend_techstore.security.dtos.AuthResponseDTO;
import com.upc.backend_techstore.security.services.CustomUserDetailsService;
import com.upc.backend_techstore.security.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j

//@CrossOrigin(origins = "${ip.frontend}")
@CrossOrigin(origins = "${ip.frontend}", allowCredentials = "true", exposedHeaders = "Authorization") //para cloud
//@CrossOrigin(origins = "*", allowedHeaders = "*", exposedHeaders = "Authorization")
@RestController
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService userDetailsService;
    private final UsuarioRepository usuarioRepository;

    public AuthController(AuthenticationManager authenticationManager, JwtUtil jwtUtil, CustomUserDetailsService userDetailsService, UsuarioRepository usuarioRepository) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
        this.usuarioRepository = usuarioRepository;
    }

    @PostMapping({"/authenticate", "/api/auth/authenticate"})
    public ResponseEntity<AuthResponseDTO> createAuthenticationToken(@RequestBody AuthRequestDTO authRequest) {
        String login = authRequest.getLogin();
        log.info("Intento de login para: {}", login);

        try {
            log.debug("Intentando autenticar con AuthenticationManager...");
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(login, authRequest.getPassword())
            );
            log.debug("Autenticación exitosa para: {}", login);
        } catch (BadCredentialsException e) {
            log.warn("❌ Credenciales inválidas para: {}", login);
            throw new BadCredentialsException("Email o contraseña incorrectos", e);
        } catch (UsernameNotFoundException e) {
            log.warn("❌ Usuario no encontrado en tabla de seguridad: {}", login);
            throw new UsernameNotFoundException("Usuario no encontrado en seguridad: " + login, e);
        }

        try {
            log.debug("Cargando UserDetails para: {}", login);
            final UserDetails userDetails = userDetailsService.loadUserByUsername(login);
            log.debug("UserDetails cargado exitosamente");
            
            final String token = jwtUtil.generateToken(userDetails);
            log.debug("JWT generado");
            
            final AuthResponseDTO authResponseDTO = buildAuthResponse(userDetails.getUsername(), token);

            Set<String> roles = userDetails.getAuthorities()
                    .stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toSet());

            HttpHeaders responseHeaders = new HttpHeaders();
            responseHeaders.set("Authorization", token);
            authResponseDTO.setRoles(roles);
            
            log.info("✅ Login exitoso para: {} - Roles: {}", login, roles);
            return ResponseEntity.ok().headers(responseHeaders).body(authResponseDTO);
            
        } catch (UsernameNotFoundException e) {
            log.error("❌ Error al cargar UserDetails para: {}", login, e);
            throw e;
        } catch (Exception e) {
            log.error("❌ Error inesperado durante login para: {}", login, e);
            throw new RuntimeException("Error durante autenticación: " + e.getMessage(), e);
        }
    }

    @GetMapping("/api/auth/me")
    public ResponseEntity<AuthResponseDTO> me(Authentication authentication) {
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            return ResponseEntity.status(401).build();
        }

        final UserDetails userDetails = userDetailsService.loadUserByUsername(authentication.getName());
        final AuthResponseDTO response = buildAuthResponse(userDetails.getUsername(), null);
        response.setRoles(userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet()));
        return ResponseEntity.ok(response);
    }

    private AuthResponseDTO buildAuthResponse(String username, String token) {
        log.debug("Construyendo AuthResponseDTO para username: {}", username);
        
        Usuario usuario = usuarioRepository.findByEmail(username)
                .orElseThrow(() -> {
                    log.error("❌ Usuario de negocio NO encontrado para email: {}. Emails disponibles en tabla usuario: {}", 
                            username, usuarioRepository.findAll().stream().map(Usuario::getEmail).collect(Collectors.toList()));
                    return new NoSuchElementException("Usuario de negocio no encontrado para el login: " + username);
                });

        log.debug("Usuario de negocio encontrado - ID: {}, Nombre: {}, Rol: {}", usuario.getId(), usuario.getNombre(), usuario.getRol());

        AuthResponseDTO authResponseDTO = new AuthResponseDTO();
        authResponseDTO.setIdUsuario(usuario.getId());
        authResponseDTO.setNombre(usuario.getNombre());
        authResponseDTO.setEmail(usuario.getEmail());
        authResponseDTO.setRol(usuario.getRol());
        authResponseDTO.setJwt(token);
        
        return authResponseDTO;
    }

}


