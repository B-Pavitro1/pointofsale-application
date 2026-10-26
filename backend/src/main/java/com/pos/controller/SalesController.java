package com.pos.controller;

import com.pos.dto.sale.CheckoutRequest;
import com.pos.dto.sale.CheckoutResponse;
import com.pos.service.CheckoutService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/sales")
@RequiredArgsConstructor
public class SalesController {

    private final CheckoutService checkoutService;

    @PostMapping("/checkout")
    @PreAuthorize("hasAnyRole('ADMIN', 'CASHIER')")
    public ResponseEntity<CheckoutResponse> checkout(
            @Valid @RequestBody CheckoutRequest request,
            Authentication authentication) {
        String cashierUsername = authentication.getName();
        CheckoutResponse response = checkoutService.processCheckout(request, cashierUsername);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/history")
    @PreAuthorize("hasAnyRole('ADMIN', 'CASHIER')")
    public ResponseEntity<List<CheckoutResponse>> getSalesHistory() {
        return ResponseEntity.ok(checkoutService.getRecentSales());
    }

    @GetMapping("/{invoiceNumber}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CASHIER')")
    public ResponseEntity<CheckoutResponse> getSaleByInvoice(@PathVariable String invoiceNumber) {
        return ResponseEntity.ok(checkoutService.getSaleByInvoice(invoiceNumber));
    }
}
