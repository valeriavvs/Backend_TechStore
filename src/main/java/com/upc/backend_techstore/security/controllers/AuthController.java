package com.upc.backend_techstore.security.controllers;

import com.upc.backend_techstore.Repository.UsuarioRepository;
import com.upc.backend_techstore.entity.Usuario;
import com.upc.backend_techstore.security.dtos.AuthRequestDTO;
import com.upc.backend_techstore.security.dtos.AuthResponseDTO;
import com.upc.backend_techstore.security.services.CustomUserDetailsService;
import com.upc.backend_techstore.security.util.JwtUtil;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Collectors;

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
        if (login == null || login.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(login, authRequest.getPassword())
        );

        final UserDetails userDetails = userDetailsService.loadUserByUsername(login);
        final String token = jwtUtil.generateToken(userDetails);
        final AuthResponseDTO authResponseDTO = buildAuthResponse(userDetails.getUsername(), token);

        Set<String> roles = userDetails.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.set("Authorization", token);
        authResponseDTO.setRoles(roles);
        return ResponseEntity.ok().headers(responseHeaders).body(authResponseDTO);
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
        Usuario usuario = usuarioRepository.findByEmail(username)
                .orElseThrow(() -> new NoSuchElementException("Usuario de negocio no encontrado para el login: " + username));

        AuthResponseDTO authResponseDTO = new AuthResponseDTO();
        authResponseDTO.setIdUsuario(usuario.getId());
        authResponseDTO.setNombre(usuario.getNombre());
        authResponseDTO.setEmail(usuario.getEmail());
        authResponseDTO.setRol(usuario.getRol());
        authResponseDTO.setJwt(token);
        return authResponseDTO;
    }

}


