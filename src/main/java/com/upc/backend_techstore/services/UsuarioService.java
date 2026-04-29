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
        Usuario usuarioEntity = modelMapper.map(usuarioDto, Usuario.class);
        // Seguridad: el rol de alta siempre es USER, aunque el cliente envie otro valor.
        usuarioEntity.setRol(ROLE_USER);
        
        // ENCRIPTAR CONTRASEÑA ANTES DE GUARDAR
        if (usuarioEntity.getPassword() != null && !usuarioEntity.getPassword().isBlank()) {
            usuarioEntity.setPassword(passwordEncoder.encode(usuarioEntity.getPassword()));
        }
        
        Usuario guardado = usuarioRepository.save(usuarioEntity);

        try {
            // Pasar la contraseña ya encriptada al servicio de seguridad
            securityUserService.upsertUserByEmail(null, guardado.getEmail(), guardado.getPassword(), guardado.getRol());
        } catch (Exception e) {
            throw new RuntimeException("No se pudo sincronizar el usuario de seguridad", e);
        }

        return modelMapper.map(guardado, UsuarioDto.class);
    }

    //ACTUALIZAR
    @Override
    public UsuarioDto actualizar(Long id, UsuarioDto usuarioDto) throws Exception {
        Usuario usuario = usuarioRepository.findById(id).orElseThrow(() -> new Exception("Usuario no encontrada"));
        String emailAnterior = usuario.getEmail();

        if (usuarioDto.getNombre() != null && !usuarioDto.getNombre().isBlank()) {
            usuario.setNombre(usuarioDto.getNombre());
        }
        if (usuarioDto.getEmail() != null && !usuarioDto.getEmail().isBlank()) {
            usuario.setEmail(usuarioDto.getEmail());
        }
        if (usuarioDto.getPassword() != null && !usuarioDto.getPassword().isBlank()) {
            // ENCRIPTAR CONTRASEÑA ANTES DE GUARDAR
            usuario.setPassword(passwordEncoder.encode(usuarioDto.getPassword()));
        }
        if (usuarioDto.getRol() != null && !usuarioDto.getRol().isBlank()) {
            usuario.setRol(normalizeRole(usuarioDto.getRol()));
        }

        Usuario actualizado = usuarioRepository.save(usuario);

        securityUserService.upsertUserByEmail(
                emailAnterior,
                actualizado.getEmail(),
                actualizado.getPassword(),
                actualizado.getRol()
        );

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

    //ELIMINAR
    @Override
    public void eliminar(Long id) throws Exception {
        Usuario usuario = usuarioRepository.findById(id).orElseThrow(() -> new Exception("Usuario no encontrada"));
        usuarioRepository.delete(usuario);
    }
}
