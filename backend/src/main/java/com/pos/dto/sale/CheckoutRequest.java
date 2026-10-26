package com.pos.dto.sale;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CheckoutRequest {

    @NotEmpty(message = "Cart cannot be empty")
    @Valid
    private List<CartItemRequest> items;

    @NotBlank(message = "Payment method is required (CASH, CARD, MOCK_GATEWAY)")
    private String paymentMethod;

    @DecimalMin(value = "0.00", message = "Discount cannot be negative")
    @Builder.Default
    private BigDecimal discountAmount = BigDecimal.ZERO;

    // Optional for Cash payments
    private BigDecimal cashTendered;
}
