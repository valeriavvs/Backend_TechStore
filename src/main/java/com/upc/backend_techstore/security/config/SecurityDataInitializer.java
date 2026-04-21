package com.upc.backend_techstore.security.config;

import com.upc.backend_techstore.Repository.UsuarioRepository;
import com.upc.backend_techstore.entity.Usuario;
import com.upc.backend_techstore.security.entities.Role;
import com.upc.backend_techstore.security.entities.User;
import com.upc.backend_techstore.security.repositories.RoleRepository;
import com.upc.backend_techstore.security.repositories.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Set;

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
                    User user = new User();
                    user.setUsername(email.trim());
                    user.setPassword(passwordEncoder.encode(usuario.getPassword()));
                    user.setRoles(Set.of(selectedRole));
                    userRepository.save(user);
                }
            }
        };
    }
}
