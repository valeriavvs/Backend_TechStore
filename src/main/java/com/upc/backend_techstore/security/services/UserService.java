package com.upc.backend_techstore.security.services;


import com.upc.backend_techstore.security.entities.Role;
import com.upc.backend_techstore.security.entities.User;
import com.upc.backend_techstore.security.repositories.RoleRepository;
import com.upc.backend_techstore.security.repositories.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Set;

@Slf4j

@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Transactional
    public void save(User user) {
        userRepository.save(user);
    }

    public Integer insertUserRol(Long user_id, Long rol_id) {
        userRepository.insertUserRol(user_id, rol_id);
        return 1;
    }

    @Transactional
    public void updateUserRole(Long userId, String roleName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("Usuario de seguridad no encontrado"));
        applySingleRole(user, roleName);
    }

    @Transactional
    public boolean syncUserRoleByUsernameIfExists(String username, String roleName) {
        if (username == null || username.isBlank() || roleName == null || roleName.isBlank()) {
            return false;
        }

        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) {
            return false;
        }

        applySingleRole(user, roleName);
        return true;
    }

    @Transactional
    public User upsertUserByEmail(String oldEmail, String newEmail, String rawOrEncodedPassword, String roleName) {
        if (newEmail == null || newEmail.isBlank()) {
            throw new IllegalArgumentException("El email es obligatorio para sincronizar seguridad");
        }

        String oldUsername = oldEmail == null ? null : oldEmail.trim();
        String newUsername = newEmail.trim();

        log.debug("Intentando sincronizar usuario - oldEmail: {}, newEmail: {}, roleName: {}", oldUsername, newUsername, roleName);

        User user = userRepository.findByUsername(newUsername)
                .orElseGet(() -> oldUsername == null ? null : userRepository.findByUsername(oldUsername).orElse(null));

        if (user == null) {
            log.info("Creando nuevo usuario de seguridad para: {}", newUsername);
            user = new User();
        } else {
            log.info("Actualizando usuario de seguridad existente: {}", newUsername);
        }

        user.setUsername(newUsername);
        if (rawOrEncodedPassword != null && !rawOrEncodedPassword.isBlank()) {
            user.setPassword(encodeIfNeeded(rawOrEncodedPassword));
            log.debug("Contraseña sincronizada para usuario: {}", newUsername);
        }

        user = applySingleRole(user, roleName);

        log.info("Usuario de seguridad sincronizado exitosamente: {} con rol: {}", newUsername, roleName);
        return user;
    }

    private User applySingleRole(User user, String roleName) {
        if (roleName == null || roleName.isBlank()) {
            throw new IllegalArgumentException("El rol es obligatorio");
        }

        String normalizedRole = normalizeRoleName(roleName);
        String cleanedRole = roleName.trim().toUpperCase();

        log.debug("Buscando rol - normalizedRole: {}, cleanedRole: {}", normalizedRole, cleanedRole);

        Role role = roleRepository.findByName(normalizedRole)
                .or(() -> {
                    log.debug("Rol {} no encontrado, intentando con {}", normalizedRole, cleanedRole);
                    return roleRepository.findByName(cleanedRole);
                })
                .orElseThrow(() -> {
                    log.error("Rol no encontrado en security. Buscaba: {}. Roles disponibles: {}",
                            normalizedRole, roleRepository.findAll());
                    return new NoSuchElementException("Rol no encontrado en security: " + normalizedRole);
                });

        log.debug("Rol encontrado: {}", normalizedRole);

        // Hibernate necesita una coleccion mutable para gestionar cambios de la relacion many-to-many.
        user.setRoles(new HashSet<>(Set.of(role)));
        User savedUser = userRepository.save(user);

        log.debug("Usuario guardado en BD - ID: {}, Username: {}, Roles: {}",
                savedUser.getId(), savedUser.getUsername(), savedUser.getRoles());

        return savedUser;
    }

    private String normalizeRoleName(String roleName) {
        String cleaned = roleName.trim().toUpperCase();
        return cleaned.startsWith("ROLE_") ? cleaned : "ROLE_" + cleaned;
    }

    private String encodeIfNeeded(String passwordValue) {
        if (passwordValue.startsWith("$2a$") || passwordValue.startsWith("$2b$") || passwordValue.startsWith("$2y$")) {
            return passwordValue;
        }
        return passwordEncoder.encode(passwordValue);
    }

}
