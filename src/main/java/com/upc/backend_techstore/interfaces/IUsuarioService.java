package com.upc.backend_techstore.interfaces;

import com.upc.backend_techstore.dto.UsuarioDto;

import java.util.List;

public interface IUsuarioService {
    List<UsuarioDto> listar();
    UsuarioDto insertar(UsuarioDto usuario);
    UsuarioDto actualizar(Long id, UsuarioDto usuarioDto) throws Exception;
    void eliminar(Long id) throws Exception;

}
