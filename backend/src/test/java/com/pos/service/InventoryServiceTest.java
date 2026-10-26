package com.pos.service;

import com.pos.dto.product.ProductResponse;
import com.pos.dto.product.StockAdjustmentRequest;
import com.pos.entity.Inventory;
import com.pos.entity.Product;
import com.pos.exception.BadRequestException;
import com.pos.repository.InventoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private ProductService productService;

    @InjectMocks
    private InventoryService inventoryService;

    private Product product;
    private Inventory inventory;

    @BeforeEach
    void setUp() {
        product = Product.builder()
                .id(1L)
                .sku("SKU-100")
                .name("Test Product")
                .price(new BigDecimal("15.00"))
                .costPrice(new BigDecimal("8.00"))
                .isActive(true)
                .build();

        inventory = Inventory.builder()
                .id(10L)
                .product(product)
                .quantity(5)
                .lowStockThreshold(10)
                .build();
    }

    @Test
    @DisplayName("Should add stock quantity when operation is ADD")
    void shouldAddStockQuantity() {
        StockAdjustmentRequest request = StockAdjustmentRequest.builder()
                .operation("ADD")
                .quantity(20)
                .build();

        when(inventoryRepository.findByProductIdWithLock(1L)).thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(inventory)).thenReturn(inventory);
        when(productService.mapToResponse(product)).thenReturn(ProductResponse.builder()
                .id(1L)
                .quantity(25)
                .build());

        ProductResponse response = inventoryService.adjustStock(1L, request);

        assertEquals(25, inventory.getQuantity()); // 5 + 20
        assertNotNull(response);
        verify(inventoryRepository).save(inventory);
    }

    @Test
    @DisplayName("Should set stock quantity when operation is SET")
    void shouldSetStockQuantity() {
        StockAdjustmentRequest request = StockAdjustmentRequest.builder()
                .operation("SET")
                .quantity(50)
                .build();

        when(inventoryRepository.findByProductIdWithLock(1L)).thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(inventory)).thenReturn(inventory);
        when(productService.mapToResponse(product)).thenReturn(ProductResponse.builder()
                .id(1L)
                .quantity(50)
                .build());

        ProductResponse response = inventoryService.adjustStock(1L, request);

        assertEquals(50, inventory.getQuantity());
        assertNotNull(response);
    }

    @Test
    @DisplayName("Should throw BadRequestException on invalid operation type")
    void shouldRejectInvalidOperation() {
        StockAdjustmentRequest request = StockAdjustmentRequest.builder()
                .operation("MULTIPLY")
                .quantity(10)
                .build();

        when(inventoryRepository.findByProductIdWithLock(1L)).thenReturn(Optional.of(inventory));

        assertThrows(BadRequestException.class, () -> {
            inventoryService.adjustStock(1L, request);
        });
    }

    @Test
    @DisplayName("Should return low stock items")
    void shouldReturnLowStockItems() {
        when(inventoryRepository.findLowStockItems()).thenReturn(List.of(inventory));
        when(productService.mapToResponse(product)).thenReturn(ProductResponse.builder()
                .id(1L)
                .lowStock(true)
                .build());

        List<ProductResponse> alerts = inventoryService.getLowStockAlerts();

        assertEquals(1, alerts.size());
        assertTrue(alerts.get(0).isLowStock());
    }
}
