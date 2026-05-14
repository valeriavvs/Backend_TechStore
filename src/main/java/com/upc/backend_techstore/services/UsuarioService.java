package com.upc.backend_techstore.services;

import com.upc.backend_techstore.Repository.UsuarioRepository;
import com.upc.backend_techstore.dto.UsuarioDto;
import com.upc.backend_techstore.entity.Usuario;
import com.upc.backend_techstore.interfaces.IUsuarioService;
import com.upc.backend_techstore.security.services.UserService;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Slf4j
@Service
public class UsuarioService implements IUsuarioService {

    private static final String ROLE_USER = "USER";
    private static final String ROLE_ADMIN = "ADMIN";

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private UserService securityUserService;

    //Listar
    @Override
    public List<UsuarioDto> listar() {
        return usuarioRepository.findAll().stream()
                .map(u -> modelMapper.map(u, UsuarioDto.class))
                .collect(Collectors.toList());
    }

    //INSERTAR
    @Override
    @Transactional
    public UsuarioDto insertar(UsuarioDto usuarioDto) {
        if (usuarioDto == null) {
            throw new IllegalArgumentException("El usuario es obligatorio");
        }

        log.info("=== Iniciando registro de usuario: {} ===", usuarioDto.getEmail());

        if (usuarioDto.getEmail() == null || usuarioDto.getEmail().isBlank()) {
            throw new IllegalArgumentException("El email es obligatorio");
        }
        if (usuarioDto.getNombre() == null || usuarioDto.getNombre().isBlank()) {
            throw new IllegalArgumentException("El nombre es obligatorio");
        }
        if (usuarioDto.getPassword() == null || usuarioDto.getPassword().isBlank()) {
            throw new IllegalArgumentException("La contraseña es obligatoria");
        }

        Usuario usuarioEntity = modelMapper.map(usuarioDto, Usuario.class);
        usuarioEntity.setEmail(usuarioDto.getEmail().trim());
        usuarioEntity.setNombre(usuarioDto.getNombre().trim());
        usuarioEntity.setPassword(encodeIfNeeded(usuarioDto.getPassword()));
        usuarioEntity.setRol(ROLE_USER);

        Usuario guardado = usuarioRepository.save(usuarioEntity);
        log.info("Usuario guardado en tabla usuario - ID: {}, Email: {}", guardado.getId(), guardado.getEmail());

        try {
            log.info("Sincronizando con tabla de seguridad (users)...");
            securityUserService.upsertUserByEmail(null, guardado.getEmail(), guardado.getPassword(), guardado.getRol());
            log.info("Usuario sincronizado correctamente en tabla users: {}", guardado.getEmail());
        } catch (Exception e) {
            log.error("ERROR al sincronizar el usuario de seguridad para: {}", guardado.getEmail(), e);
            throw new RuntimeException("No se pudo sincronizar el usuario de seguridad", e);
        }

        return modelMapper.map(guardado, UsuarioDto.class);
    }

    //ACTUALIZAR
    @Override
    @Transactional
    public UsuarioDto actualizar(Long id, UsuarioDto usuarioDto) throws Exception {
        if (usuarioDto == null) {
            throw new IllegalArgumentException("El usuario es obligatorio");
        }

        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Usuario no encontrado"));

        String emailAnterior = usuario.getEmail();

        if (usuarioDto.getNombre() != null && !usuarioDto.getNombre().isBlank()) {
            usuario.setNombre(usuarioDto.getNombre().trim());
            log.debug("Nombre actualizado a: {}", usuarioDto.getNombre());
        }

        if (usuarioDto.getEmail() != null && !usuarioDto.getEmail().isBlank()) {
            usuario.setEmail(usuarioDto.getEmail().trim());
            log.debug("Email actualizado de {} a {}", emailAnterior, usuarioDto.getEmail());
        }

        if (usuarioDto.getPassword() != null && !usuarioDto.getPassword().isBlank()) {
            usuario.setPassword(encodeIfNeeded(usuarioDto.getPassword()));
            log.debug("Contraseña actualizada y encriptada para usuario ID: {}", id);
        }

        if (usuarioDto.getRol() != null && !usuarioDto.getRol().isBlank()) {
            usuario.setRol(normalizeRole(usuarioDto.getRol()));
            log.debug("Rol actualizado a: {}", usuario.getRol());
        }

        Usuario actualizado = usuarioRepository.save(usuario);
        log.info("Usuario actualizado en tabla usuario - ID: {}, Email: {}", actualizado.getId(), actualizado.getEmail());

        try {
            securityUserService.upsertUserByEmail(
                    emailAnterior,
                    actualizado.getEmail(),
                    actualizado.getPassword(),
                    actualizado.getRol()
            );
            log.info("Usuario sincronizado exitosamente en tabla users");
        } catch (Exception e) {
            log.error("ERROR al sincronizar cambios de seguridad para usuario ID: {}", id, e);
            throw new RuntimeException("No se pudo sincronizar los cambios: " + e.getMessage(), e);
        }

        return modelMapper.map(actualizado, UsuarioDto.class);
    }

    //ELIMINAR
    @Override
    public void eliminar(Long id) throws Exception {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new Exception("Usuario no encontrada"));
        usuarioRepository.delete(usuario);
    }

    private String encodeIfNeeded(String passwordValue) {
        if (passwordValue == null) {
            return null;
        }
        if (passwordValue.startsWith("$2a$") || passwordValue.startsWith("$2b$") || passwordValue.startsWith("$2y$")) {
            return passwordValue;
        }
        return passwordEncoder.encode(passwordValue);
    }

    private String normalizeRole(String roleValue) {
        String role = roleValue.trim().toUpperCase();
        if (role.startsWith("ROLE_")) {
            role = role.substring("ROLE_".length());
        }

        if (!ROLE_USER.equals(role) && !ROLE_ADMIN.equals(role)) {
            throw new IllegalArgumentException("Rol no valido. Use USER o ADMIN");
        }

        return role;
    }
}
