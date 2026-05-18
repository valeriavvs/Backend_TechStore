package com.upc.backend_techstore.controllers;

import com.upc.backend_techstore.dto.PaymentRequestDto;
import com.upc.backend_techstore.dto.PaymentResponseDto;
import com.upc.backend_techstore.services.PaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public ResponseEntity<PaymentResponseDto> createPayment(@RequestBody PaymentRequestDto request) {
        return ResponseEntity.ok(paymentService.processCardPayment(request));
    }
}

