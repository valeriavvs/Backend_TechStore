package com.upc.backend_techstore.security.config;

import com.upc.backend_techstore.Repository.UsuarioRepository;
import com.upc.backend_techstore.entity.Usuario;
import com.upc.backend_techstore.security.entities.Role;
import com.upc.backend_techstore.security.entities.User;
import com.upc.backend_techstore.security.repositories.RoleRepository;
import com.upc.backend_techstore.security.repositories.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Set;

@Slf4j
@Configuration
public class SecurityDataInitializer {

    @Bean
    CommandLineRunner seedSecurityData(
            RoleRepository roleRepository,
            UserRepository userRepository,
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder
    ) {
        return args -> {
            log.info("=== Iniciando sincronización de seguridad y encriptación de contraseñas ===");
            
            Role roleUser = roleRepository.findByName("ROLE_USER")
                    .orElseGet(() -> {
                        Role role = new Role();
                        role.setName("ROLE_USER");
                        return roleRepository.save(role);
                    });

            Role roleAdmin = roleRepository.findByName("ROLE_ADMIN")
                    .orElseGet(() -> {
                        Role role = new Role();
                        role.setName("ROLE_ADMIN");
                        return roleRepository.save(role);
                    });

            // PASO 1: Encriptar contraseñas de Usuario que están en texto plano
            log.info("--- PASO 1: Detectando contraseñas en texto plano ---");
            for (Usuario usuario : usuarioRepository.findAll()) {
                if (usuario.getPassword() != null && !isAlreadyEncoded(usuario.getPassword())) {
                    log.warn("⚠️ Detectada contraseña en texto plano para usuario: {}, encriptando...", usuario.getEmail());
                    usuario.setPassword(passwordEncoder.encode(usuario.getPassword()));
                    usuarioRepository.save(usuario);
                    log.info("✅ Contraseña encriptada para: {}", usuario.getEmail());
                }
            }

            // PASO 2: Sincronizar tabla de seguridad desde tabla Usuario
            log.info("--- PASO 2: Sincronizando usuarios con tabla de seguridad (users) ---");
            for (Usuario usuario : usuarioRepository.findAll()) {
                String email = usuario.getEmail();
                if (email == null || email.isBlank()) {
                    continue;
                }

                Role selectedRole = "ADMIN".equalsIgnoreCase(usuario.getRol())
                        || "ROLE_ADMIN".equalsIgnoreCase(usuario.getRol())
                        ? roleAdmin
                        : roleUser;

                if (userRepository.findByUsername(email).isEmpty()) {
                    log.info("Creando usuario de seguridad para: {} con rol: {}", email, selectedRole.getName());
                    User user = new User();
                    user.setUsername(email.trim());
                    user.setPassword(usuario.getPassword());  // Ya está encriptada
                    user.setRoles(Set.of(selectedRole));
                    User savedUser = userRepository.save(user);
                    log.info("✅ Usuario de seguridad creado - ID: {}, Username: {}", savedUser.getId(), savedUser.getUsername());
                } else {
                    log.debug("Usuario de seguridad ya existe para: {}", email);
                }
            }
            log.info("=== ✅ Sincronización de seguridad completada ===");
        };
    }

    /**
     * Verifica si una contraseña ya está encriptada con BCrypt
     */
    private boolean isAlreadyEncoded(String password) {
        return password != null && 
               (password.startsWith("$2a$") || 
                password.startsWith("$2b$") || 
                password.startsWith("$2y$"));
    }
}
