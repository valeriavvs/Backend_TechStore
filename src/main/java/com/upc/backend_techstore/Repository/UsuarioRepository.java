package com.upc.backend_techstore.Repository;

import com.upc.backend_techstore.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsuarioRepository  extends JpaRepository<Usuario, Long> {
	Optional<Usuario> findByEmail(String email);
}
