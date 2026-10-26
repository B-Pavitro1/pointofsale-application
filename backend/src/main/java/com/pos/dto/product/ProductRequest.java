package com.pos.dto.product;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductRequest {

    @NotBlank(message = "SKU is required")
    @Size(max = 64, message = "SKU cannot exceed 64 characters")
    private String sku;

    @Size(max = 64, message = "Barcode cannot exceed 64 characters")
    private String barcode;

    @NotBlank(message = "Product name is required")
    @Size(max = 150, message = "Product name cannot exceed 150 characters")
    private String name;

    @NotBlank(message = "Category is required")
    @Size(max = 100, message = "Category cannot exceed 100 characters")
    private String category;

    @NotNull(message = "Selling price is required")
    @DecimalMin(value = "0.01", message = "Price must be greater than zero")
    private BigDecimal price;

    @NotNull(message = "Cost price is required")
    @DecimalMin(value = "0.00", message = "Cost price cannot be negative")
    private BigDecimal costPrice;

    @Min(value = 0, message = "Initial quantity cannot be negative")
    @Builder.Default
    private Integer initialQuantity = 0;

    @Min(value = 1, message = "Low stock threshold must be at least 1")
    @Builder.Default
    private Integer lowStockThreshold = 10;
}
