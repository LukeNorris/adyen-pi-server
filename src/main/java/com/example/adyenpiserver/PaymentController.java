package com.example.adyenpiserver;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.adyenpiserver.dto.PaymentRequestDTO;
import com.example.adyenpiserver.dto.PaymentResponseDTO;
import com.example.adyenpiserver.service.PaymentService;

@RestController
@RequestMapping("/api")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @PostMapping("/payment")
    public PaymentResponseDTO initiatePayment(@RequestBody PaymentRequestDTO requestDTO) {
        try {
            return paymentService.processPayment(requestDTO);
        } catch (Exception e) {
            return new PaymentResponseDTO("ERROR", e.getMessage());
        }
    }
}
