package com.upc.backend_techstore.Repository;

import com.upc.backend_techstore.entity.CarritoItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CarritoItemRepository extends JpaRepository<CarritoItem, Long> {
    List<CarritoItem> findByCarritoId(Long carritoId);
    Optional<CarritoItem> findByCarritoIdAndProductoId(Long carritoId, Long productoId);
}
