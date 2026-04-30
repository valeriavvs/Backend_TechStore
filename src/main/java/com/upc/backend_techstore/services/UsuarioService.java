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

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class UsuarioService implements IUsuarioService {

    private static final String ROLE_USER = "USER";
    private static final String ROLE_ADMIN = "ADMIN";

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private UserService securityUserService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public List<UsuarioDto> listar() {
        return usuarioRepository.findAll()
                .stream()
                .map(u -> modelMapper.map(u, UsuarioDto.class))
                .collect(Collectors.toList());
    }

    @Override
    public UsuarioDto insertar(UsuarioDto usuarioDto) {
        log.info("=== Iniciando registro de usuario: {} ===", usuarioDto.getEmail());

        Usuario usuarioEntity = modelMapper.map(usuarioDto, Usuario.class);
        usuarioEntity.setRol(ROLE_USER);

        if (usuarioEntity.getPassword() != null && !usuarioEntity.getPassword().isBlank()) {
            usuarioEntity.setPassword(passwordEncoder.encode(usuarioEntity.getPassword()));
            log.debug("Contraseña encriptada para usuario: {}", usuarioDto.getEmail());
        }

        Usuario guardado = usuarioRepository.save(usuarioEntity);
        log.info("Usuario guardado en tabla usuario - ID: {}, Email: {}", guardado.getId(), guardado.getEmail());

        try {
            log.info("Sincronizando con tabla de seguridad (users)...");
            securityUserService.upsertUserByEmail(
                    null,
                    guardado.getEmail(),
                    guardado.getPassword(),
                    guardado.getRol()
            );
            log.info("Usuario sincronizado correctamente en tabla users: {}", guardado.getEmail());
        } catch (Exception e) {
            log.error("ERROR al sincronizar el usuario de seguridad para: {}", guardado.getEmail(), e);
            throw new RuntimeException("No se pudo sincronizar el usuario de seguridad: " + e.getMessage(), e);
        }

        log.info("=== Registro completado para: {} ===", usuarioDto.getEmail());
        return modelMapper.map(guardado, UsuarioDto.class);
    }

    @Override
    public UsuarioDto actualizar(Long id, UsuarioDto usuarioDto) throws Exception {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new Exception("Usuario no encontrado"));

        String emailAnterior = usuario.getEmail();

        if (usuarioDto.getNombre() != null && !usuarioDto.getNombre().isBlank()) {
            usuario.setNombre(usuarioDto.getNombre());
            log.debug("Nombre actualizado a: {}", usuarioDto.getNombre());
        }

        if (usuarioDto.getEmail() != null && !usuarioDto.getEmail().isBlank()) {
            usuario.setEmail(usuarioDto.getEmail().trim());
            log.debug("Email actualizado de {} a {}", emailAnterior, usuarioDto.getEmail());
        }

        if (usuarioDto.getPassword() != null && !usuarioDto.getPassword().isBlank()) {
            usuario.setPassword(passwordEncoder.encode(usuarioDto.getPassword()));
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

    @Override
    public void eliminar(Long id) throws Exception {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new Exception("Usuario no encontrado"));
        usuarioRepository.delete(usuario);
    }
}
