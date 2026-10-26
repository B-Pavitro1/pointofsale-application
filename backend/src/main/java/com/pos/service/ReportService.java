package com.pos.service;

import com.pos.dto.sale.SalesSummaryResponse;
import com.pos.repository.InventoryRepository;
import com.pos.repository.ProductRepository;
import com.pos.repository.SaleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final SaleRepository saleRepository;
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;

    @Transactional(readOnly = true)
    public SalesSummaryResponse getSalesSummary() {
        long salesCount = saleRepository.countCompletedSales();
        BigDecimal revenue = saleRepository.sumTotalRevenue();
        long productCount = productRepository.count();
        long lowStockCount = inventoryRepository.findLowStockItems().size();

        return SalesSummaryResponse.builder()
                .totalSalesCount(salesCount)
                .totalRevenue(revenue != null ? revenue : BigDecimal.ZERO)
                .totalProductsCount(productCount)
                .lowStockCount(lowStockCount)
                .build();
    }
}
