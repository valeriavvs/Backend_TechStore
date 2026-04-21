package com.upc.backend_techstore.Repository;

import com.upc.backend_techstore.entity.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {
    List<Pedido> findByUsuarioId(Long idUsuario);



}
