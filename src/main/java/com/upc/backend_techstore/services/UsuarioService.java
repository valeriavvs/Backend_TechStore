package com.upc.backend_techstore.services;

import com.upc.backend_techstore.Repository.UsuarioRepository;
import com.upc.backend_techstore.dto.UsuarioDto;
import com.upc.backend_techstore.entity.Usuario;
import com.upc.backend_techstore.interfaces.IUsuarioService;
import com.upc.backend_techstore.security.services.UserService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j

@Service

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

    //Listar
    @Override
    public List<UsuarioDto> listar() {
        return usuarioRepository.findAll()
                .stream()
                .map(u -> modelMapper.map(u, UsuarioDto.class))
                .collect(Collectors.toList());
    }

    //INSERTAR
    @Override
    public UsuarioDto insertar(UsuarioDto usuarioDto) {
        log.info("=== Iniciando registro de usuario: {} ===", usuarioDto.getEmail());
        
        usuarioEntity.setRol(ROLE_USER);
        
        // ENCRIPTAR CONTRASEÑA ANTES DE GUARDAR
        if (usuarioEntity.getPassword() != null && !usuarioEntity.getPassword().isBlank()) {
            String passwordEncriptada = passwordEncoder.encode(usuarioEntity.getPassword());
            usuarioEntity.setPassword(passwordEncriptada);
            log.debug("Contraseña encriptada para usuario: {}", usuarioDto.getEmail());
        }
        
            usuarioEntity.setPassword(passwordEncoder.encode(usuarioEntity.getPassword()));

        try {
            log.info("Sincronizando con tabla de seguridad (users)...");
            // Pasar la contraseña ya encriptada al servicio de seguridad
        } catch (Exception e) {
            log.error("ERROR al sincronizar el usuario de seguridad para: {}", guardado.getEmail(), e);
            throw new RuntimeException("No se pudo sincronizar el usuario de seguridad: " + e.getMessage(), e);

        log.info("=== Registro completado para: {} ===", usuarioDto.getEmail());
        return modelMapper.map(guardado, UsuarioDto.class);
    }

    //ACTUALIZAR
    @Override
    public UsuarioDto actualizar(Long id, UsuarioDto usuarioDto) throws Exception {
        
            throw new RuntimeException("No se pudo sincronizar el usuario de seguridad", e);
            usuario.setNombre(usuarioDto.getNombre());
            log.debug("Nombre actualizado a: {}", usuarioDto.getNombre());
        }
        if (usuarioDto.getEmail() != null && !usuarioDto.getEmail().isBlank()) {
            log.debug("Email actualizado de {} a {}", emailAnterior, usuarioDto.getEmail());
        }
        if (usuarioDto.getPassword() != null && !usuarioDto.getPassword().isBlank()) {
            // ENCRIPTAR CONTRASEÑA ANTES DE GUARDAR
        }
        if (usuarioDto.getRol() != null && !usuarioDto.getRol().isBlank()) {
            usuario.setRol(normalizeRole(usuarioDto.getRol()));
            log.debug("Rol actualizado a: {}", usuario.getRol());
        }

        Usuario actualizado = usuarioRepository.save(usuario);
        log.info("Usuario actualizado en tabla usuario - ID: {}, Email: {}", actualizado.getId(), actualizado.getEmail());

        securityUserService.upsertUserByEmail(
                emailAnterior,
                actualizado.getEmail(),
                actualizado.getPassword(),
                actualizado.getRol()
        );
        try {
            securityUserService.upsertUserByEmail(
                    emailAnterior,
                    actualizado.getEmail(),
                    actualizado.getPassword(),
                    actualizado.getRol()
            log.info("✅ Usuario sincronizado exitosamente");
        } catch (Exception e) {
            log.error("ERROR al sincronizar cambios de seguridad para usuario ID: {}", id, e);
            throw new RuntimeException("No se pudo sincronizar los cambios: " + e.getMessage(), e);
        }

        return modelMapper.map(actualizado, UsuarioDto.class);
    }

    private String normalizeRole(String roleValue) {
        String role = roleValue.trim().toUpperCase();
        if (role.startsWith("ROLE_")) {
        }

        if (!ROLE_USER.equals(role) && !ROLE_ADMIN.equals(role)) {
            throw new IllegalArgumentException("Rol no valido. Use USER o ADMIN");

        return role;
    }

    //ELIMINAR
    @Override
    public void eliminar(Long id) throws Exception {
        Usuario usuario = usuarioRepository.findById(id).orElseThrow(() -> new Exception("Usuario no encontrada"));
        usuarioRepository.delete(usuario);
    }
}
