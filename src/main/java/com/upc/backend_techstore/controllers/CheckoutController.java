package com.upc.backend_techstore.controllers;

import com.upc.backend_techstore.dto.CheckoutRequestDto;
import com.upc.backend_techstore.dto.CheckoutResponseDto;
import com.upc.backend_techstore.services.CheckoutService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/checkout")
public class CheckoutController {

    @Autowired
    private CheckoutService checkoutService;

    @PostMapping("/confirmar")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public ResponseEntity<CheckoutResponseDto> confirmarCompra(
            @RequestBody CheckoutRequestDto request,
            Authentication authentication) {
        return ResponseEntity.ok(checkoutService.confirmarCompra(request, authentication.getName()));
    }
}

