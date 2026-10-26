package com.pos.dto.sale;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalesSummaryResponse {

    private long totalSalesCount;
    private BigDecimal totalRevenue;
    private long totalProductsCount;
    private long lowStockCount;
}
