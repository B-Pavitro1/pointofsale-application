package com.pos.dto.product;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductResponse {

    private Long id;
    private String sku;
    private String barcode;
    private String name;
    private String category;
    private BigDecimal price;
    private BigDecimal costPrice;
    private Integer quantity;
    private Integer lowStockThreshold;
    private boolean lowStock;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
