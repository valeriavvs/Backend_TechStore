package com.upc.backend_techstore.security.dtos;


import java.util.Set;

@lombok.Data
public class AuthResponseDTO {
    private Long idUsuario;
    private String nombre;
    private String email;
    private String rol;
    private String jwt;
    private Set<String> roles;
}