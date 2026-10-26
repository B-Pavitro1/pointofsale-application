package com.pos.dto.product;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockAdjustmentRequest {

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Adjustment quantity must be at least 1")
    private Integer quantity;

    @NotBlank(message = "Operation type is required (ADD or SET)")
    private String operation; // ADD or SET

    private String reason;
}
