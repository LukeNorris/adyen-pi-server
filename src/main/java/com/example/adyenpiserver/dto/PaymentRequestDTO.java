package com.example.adyenpiserver.dto;

public record PaymentRequestDTO(
    double amount, 
    String currency
) {}
