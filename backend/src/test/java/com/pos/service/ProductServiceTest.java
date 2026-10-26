package com.pos.service;

import com.pos.dto.product.ProductRequest;
import com.pos.dto.product.ProductResponse;
import com.pos.entity.Inventory;
import com.pos.entity.Product;
import com.pos.exception.BadRequestException;
import com.pos.exception.ResourceNotFoundException;
import com.pos.repository.InventoryRepository;
import com.pos.repository.ProductRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private InventoryRepository inventoryRepository;

    @InjectMocks
    private ProductService productService;

    @Test
    @DisplayName("Should create product and initial inventory record")
    void shouldCreateProductSuccessfully() {
        ProductRequest request = ProductRequest.builder()
                .sku("SNK-001")
                .name("Sea Salt Chips")
                .category("Snacks")
                .price(new BigDecimal("3.50"))
                .costPrice(new BigDecimal("1.50"))
                .initialQuantity(50)
                .lowStockThreshold(10)
                .build();

        when(productRepository.existsBySku("SNK-001")).thenReturn(false);
        when(productRepository.save(any(Product.class))).thenAnswer(i -> {
            Product p = i.getArgument(0);
            p.setId(1L);
            return p;
        });

        ProductResponse response = productService.createProduct(request);

        assertNotNull(response);
        assertEquals("SNK-001", response.getSku());
        assertEquals("Sea Salt Chips", response.getName());
        assertEquals(50, response.getQuantity());
        verify(inventoryRepository).save(any(Inventory.class));
    }

    @Test
    @DisplayName("Should throw BadRequestException if SKU already exists")
    void shouldRejectDuplicateSku() {
        ProductRequest request = ProductRequest.builder()
                .sku("DUPLICATE-SKU")
                .name("Sample Item")
                .category("Snacks")
                .price(new BigDecimal("3.50"))
                .costPrice(new BigDecimal("1.50"))
                .build();

        when(productRepository.existsBySku("DUPLICATE-SKU")).thenReturn(true);

        assertThrows(BadRequestException.class, () -> {
            productService.createProduct(request);
        });

        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    @DisplayName("Should soft-delete product by setting isActive to false")
    void shouldSoftDeleteProduct() {
        Product product = Product.builder()
                .id(5L)
                .sku("DEL-001")
                .name("Delete Me")
                .isActive(true)
                .build();

        when(productRepository.findById(5L)).thenReturn(Optional.of(product));

        productService.deleteProduct(5L);

        assertFalse(product.getIsActive());
        verify(productRepository).save(product);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when product does not exist")
    void shouldThrowExceptionWhenNotFound() {
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            productService.getProductById(999L);
        });
    }
}
