package com.upc.backend_techstore.security.services;


import com.upc.backend_techstore.security.entities.Role;
import com.upc.backend_techstore.security.entities.User;
import com.upc.backend_techstore.security.repositories.RoleRepository;
import com.upc.backend_techstore.security.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Set;

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
    public void upsertUserByEmail(String oldEmail, String newEmail, String rawOrEncodedPassword, String roleName) {
        if (newEmail == null || newEmail.isBlank()) {
            throw new IllegalArgumentException("El email es obligatorio para sincronizar seguridad");
        }

        String oldUsername = oldEmail == null ? null : oldEmail.trim();
        String newUsername = newEmail.trim();

        User user = userRepository.findByUsername(newUsername)
                .orElseGet(() -> oldUsername == null ? null : userRepository.findByUsername(oldUsername).orElse(null));

        if (user == null) {
            user = new User();
        }

        user.setUsername(newUsername);
        if (rawOrEncodedPassword != null && !rawOrEncodedPassword.isBlank()) {
            user.setPassword(encodeIfNeeded(rawOrEncodedPassword));
        }

        applySingleRole(user, roleName);
    }

    private void applySingleRole(User user, String roleName) {
        if (roleName == null || roleName.isBlank()) {
            throw new IllegalArgumentException("El rol es obligatorio");
        }

        String normalizedRole = normalizeRoleName(roleName);
        String cleanedRole = roleName.trim().toUpperCase();

        Role role = roleRepository.findByName(normalizedRole)
                .or(() -> roleRepository.findByName(cleanedRole))
                .orElseThrow(() -> new NoSuchElementException("Rol no encontrado en security: " + normalizedRole));

        // Hibernate necesita una coleccion mutable para gestionar cambios de la relacion many-to-many.
        user.setRoles(new HashSet<>(Set.of(role)));
        userRepository.save(user);
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
