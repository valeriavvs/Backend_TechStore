package com.upc.backend_techstore.security.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.upc.backend_techstore.Repository.UsuarioRepository;
import com.upc.backend_techstore.entity.Usuario;
import com.upc.backend_techstore.security.dtos.AuthResponseDTO;
import com.upc.backend_techstore.security.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
public class GoogleAuthService {

    private static final URI GOOGLE_TOKENINFO_URI = URI.create("https://oauth2.googleapis.com/tokeninfo");
    private static final String ROLE_USER = "USER";

    private final UsuarioRepository usuarioRepository;
    private final UserService userService;
    private final CustomUserDetailsService customUserDetailsService;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient;

    @Value("${google.oauth.client-id}")
    private String googleClientId;

    public GoogleAuthService(UsuarioRepository usuarioRepository,
                             UserService userService,
                             CustomUserDetailsService customUserDetailsService,
                             JwtUtil jwtUtil,
                             PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.userService = userService;
        this.customUserDetailsService = customUserDetailsService;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = passwordEncoder;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    @Transactional
    public AuthResponseDTO authenticateWithGoogle(String idToken) {
        String normalizedToken = sanitize(idToken);
        if (normalizedToken == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El idToken de Google es obligatorio");
        }

        log.info("Iniciando autenticación con Google. idTokenPrefix={}", tokenPrefix(normalizedToken));

        JsonNode payload = validateWithGoogleTokenInfo(normalizedToken);
        String audience = text(payload, "aud");
        if (audience == null || !audience.equals(googleClientId)) {
            log.warn("Token de Google rechazado por audiencia inválida. aud={}, clientId={}", audience, googleClientId);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token de Google inválido");
        }

        if (!payload.path("email_verified").asBoolean(false)) {
            log.warn("Token de Google rechazado porque el email no está verificado");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "El email de Google no está verificado");
        }

        String email = sanitize(text(payload, "email"));
        if (email == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Google no devolvió un email válido");
        }

        String name = sanitize(text(payload, "name"));
        if (name == null) {
            name = email.contains("@") ? email.substring(0, email.indexOf('@')) : email;
        }

        String picture = sanitize(text(payload, "picture"));
        if (picture != null) {
            log.debug("Google devolvió picture para {}", email);
        }

        String issuer = sanitize(text(payload, "iss"));
        if (issuer != null && !issuer.equals("accounts.google.com") && !issuer.equals("https://accounts.google.com")) {
            log.warn("Token de Google rechazado por issuer inválido: {}", issuer);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token de Google inválido");
        }

        Usuario usuario = upsertBusinessUser(email, name);
        userService.upsertUserByEmail(null, usuario.getEmail(), usuario.getPassword(), usuario.getRol());

        UserDetails userDetails;
        try {
            userDetails = customUserDetailsService.loadUserByUsername(usuario.getEmail());
        } catch (UsernameNotFoundException ex) {
            log.error("No se pudo cargar el usuario de seguridad después de sincronizar Google para {}", email, ex);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo sincronizar el usuario de seguridad");
        }

        String jwt = jwtUtil.generateToken(userDetails);
        Set<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        AuthResponseDTO response = new AuthResponseDTO();
        response.setIdUsuario(usuario.getId());
        response.setNombre(usuario.getNombre());
        response.setEmail(usuario.getEmail());
        response.setRol(usuario.getRol());
        response.setJwt(jwt);
        response.setRoles(roles);

        log.info("Login con Google exitoso para {} con rol {}", email, usuario.getRol());
        return response;
    }

    private JsonNode validateWithGoogleTokenInfo(String idToken) {
        try {
            String url = GOOGLE_TOKENINFO_URI + "?id_token=" + URLEncoder.encode(idToken, StandardCharsets.UTF_8);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                log.warn("Google tokeninfo respondió status {}", response.statusCode());
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token de Google inválido");
            }

            return objectMapper.readTree(response.body());
        } catch (IOException ex) {
            log.error("Error leyendo respuesta de Google tokeninfo", ex);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No se pudo validar el token de Google");
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            log.error("La validación con Google fue interrumpida", ex);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No se pudo validar el token de Google");
        }
    }

    private Usuario upsertBusinessUser(String email, String name) {
        Usuario usuario = usuarioRepository.findByEmail(email).orElseGet(Usuario::new);
        boolean isNew = usuario.getId() == null;

        usuario.setEmail(email);
        if (usuario.getNombre() == null || usuario.getNombre().isBlank()) {
            usuario.setNombre(name);
        }
        if (usuario.getRol() == null || usuario.getRol().isBlank()) {
            usuario.setRol(ROLE_USER);
        }
        if (isNew || usuario.getPassword() == null || usuario.getPassword().isBlank()) {
            usuario.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
        }

        Usuario saved = usuarioRepository.save(usuario);
        log.debug("Usuario de negocio sincronizado por Google: id={}, email={}, rol={}", saved.getId(), saved.getEmail(), saved.getRol());
        return saved;
    }

    private String text(JsonNode node, String field) {
        if (node == null || field == null) {
            return null;
        }
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return null;
        }
        String text = value.asText();
        return sanitize(text);
    }

    private String sanitize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String tokenPrefix(String token) {
        int length = Math.min(12, token.length());
        return token.substring(0, length);
    }
}


