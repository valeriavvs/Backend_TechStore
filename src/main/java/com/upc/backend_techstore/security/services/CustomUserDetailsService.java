package com.upc.backend_techstore.security.services;


import com.upc.backend_techstore.security.entities.User;
import com.upc.backend_techstore.security.repositories.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.debug("Buscando usuario en tabla users con username: {}", username);
        
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.error("❌ Usuario NO encontrado en tabla users para username: {}. Usuarios disponibles: {}", 
                            username, userRepository.findAll().stream().map(User::getUsername).collect(Collectors.toList()));
                    return new UsernameNotFoundException("Usuario no encontrado en tabla users: " + username);
                });

        log.debug("Usuario encontrado en tabla users - ID: {}, Username: {}", user.getId(), user.getUsername());

        Set<GrantedAuthority> authorities = user.getRoles().stream()
                .map(role -> {
                    String roleName = role.getName();
                    String normalized = roleName.startsWith("ROLE_") ? roleName : "ROLE_" + roleName;
                    log.debug("Rol mapeado: {} -> {}", roleName, normalized);
                    return new SimpleGrantedAuthority(normalized);
                })
                .collect(Collectors.toSet());

        log.debug("Autoridades asignadas para {}: {}", username, authorities);

        UserDetails userDetails = org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername())
                .password(user.getPassword())
                .authorities(authorities)
                .build();
        
        log.debug("UserDetails construido exitosamente para: {}", username);
        return userDetails;
    }
}
