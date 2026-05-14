package com.upc.backend_techstore.security.dtos;

public class GoogleAuthRequestDTO {
    private String idToken;

    public String getIdToken() {
        return idToken;
    }

    public void setIdToken(String idToken) {
        this.idToken = idToken;
    }
}

