package com.upc.backend_techstore.interfaces;

import com.upc.backend_techstore.dto.PedidoDto;

import java.util.List;

public interface IPedidoService {
    PedidoDto guardar(PedidoDto pedidoDto);
    List<PedidoDto> listar();
    PedidoDto actualizarEstado(Long id, String estado) throws Exception;
    List<PedidoDto> listarPorUsarioId(Long idUsario);
}
