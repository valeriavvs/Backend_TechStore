package com.upc.backend_techstore.interfaces;

import com.upc.backend_techstore.dto.DetallePedidoDto;

import java.util.List;

public interface IDetallePedidoService {
        DetallePedidoDto guardar(DetallePedidoDto detallePedidoDto) throws Exception;
        List<DetallePedidoDto> listar();
        DetallePedidoDto actualizar(Long id, DetallePedidoDto detallePedidoDto) throws Exception;
        void eliminar(Long id) throws Exception;
}
