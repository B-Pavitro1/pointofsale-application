package com.pos.service;

import com.pos.dto.sale.CartItemRequest;
import com.pos.dto.sale.CheckoutRequest;
import com.pos.dto.sale.CheckoutResponse;
import com.pos.entity.*;
import com.pos.exception.BadRequestException;
import com.pos.exception.InsufficientStockException;
import com.pos.repository.InventoryRepository;
import com.pos.repository.ProductRepository;
import com.pos.repository.SaleRepository;
import com.pos.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CheckoutServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private SaleRepository saleRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PaymentService paymentService;

    @InjectMocks
    private CheckoutService checkoutService;

    private User cashier;
    private Product espresso;
    private Inventory inventory;

    @BeforeEach
    void setUp() {
        cashier = User.builder()
                .id(1L)
                .username("cashier")
                .fullName("Jane Cashier")
                .isActive(true)
                .build();

        espresso = Product.builder()
                .id(10L)
                .sku("BEV-COF-001")
                .name("Espresso Dark Roast")
                .category("Beverages")
                .price(new BigDecimal("10.00"))
                .costPrice(new BigDecimal("5.00"))
                .isActive(true)
                .build();

        inventory = Inventory.builder()
                .id(100L)
                .product(espresso)
                .quantity(20)
                .lowStockThreshold(5)
                .build();
    }

    @Test
    @DisplayName("Should successfully process checkout, deduct stock, and generate receipt")
    void shouldProcessCheckoutSuccessfully() {
        CheckoutRequest request = CheckoutRequest.builder()
                .items(List.of(new CartItemRequest(10L, 2)))
                .paymentMethod("CASH")
                .cashTendered(new BigDecimal("25.00"))
                .discountAmount(BigDecimal.ZERO)
                .build();

        when(userRepository.findByUsername("cashier")).thenReturn(Optional.of(cashier));
        when(productRepository.findById(10L)).thenReturn(Optional.of(espresso));
        when(inventoryRepository.findByProductIdWithLock(10L)).thenReturn(Optional.of(inventory));
        when(paymentService.processPayment(eq("CASH"), any(BigDecimal.class))).thenReturn("CASH-TXN-1234");
        when(saleRepository.save(any(Sale.class))).thenAnswer(invocation -> {
            Sale s = invocation.getArgument(0);
            s.setId(500L);
            s.setCreatedAt(LocalDateTime.now());
            return s;
        });

        CheckoutResponse response = checkoutService.processCheckout(request, "cashier");

        assertNotNull(response);
        assertEquals("COMPLETED", response.getStatus());
        assertEquals("Jane Cashier", response.getCashierName());
        assertEquals(new BigDecimal("20.00"), response.getSubtotal()); // 2 * $10.00
        assertEquals(new BigDecimal("1.60"), response.getTaxAmount()); // 8% tax on $20.00
        assertEquals(new BigDecimal("21.60"), response.getTotalAmount());
        assertEquals(new BigDecimal("3.40"), response.getChangeDue()); // $25.00 - $21.60

        // Verify stock deducted from 20 down to 18
        assertEquals(18, inventory.getQuantity());
        verify(inventoryRepository).save(inventory);
        verify(saleRepository).save(any(Sale.class));
    }

    @Test
    @DisplayName("Should throw InsufficientStockException and reject checkout when stock is inadequate")
    void shouldThrowExceptionWhenInsufficientStock() {
        CheckoutRequest request = CheckoutRequest.builder()
                .items(List.of(new CartItemRequest(10L, 25))) // Requesting 25 when only 20 in stock
                .paymentMethod("CASH")
                .discountAmount(BigDecimal.ZERO)
                .build();

        when(userRepository.findByUsername("cashier")).thenReturn(Optional.of(cashier));
        when(productRepository.findById(10L)).thenReturn(Optional.of(espresso));
        when(inventoryRepository.findByProductIdWithLock(10L)).thenReturn(Optional.of(inventory));

        assertThrows(InsufficientStockException.class, () -> {
            checkoutService.processCheckout(request, "cashier");
        });

        // Stock must remain unchanged
        assertEquals(20, inventory.getQuantity());
        verify(saleRepository, never()).save(any(Sale.class));
    }

    @Test
    @DisplayName("Should reject purchase when product is inactive")
    void shouldRejectInactiveProduct() {
        espresso.setIsActive(false);

        CheckoutRequest request = CheckoutRequest.builder()
                .items(List.of(new CartItemRequest(10L, 1)))
                .paymentMethod("CASH")
                .build();

        when(userRepository.findByUsername("cashier")).thenReturn(Optional.of(cashier));
        when(productRepository.findById(10L)).thenReturn(Optional.of(espresso));

        assertThrows(BadRequestException.class, () -> {
            checkoutService.processCheckout(request, "cashier");
        });

        verify(inventoryRepository, never()).findByProductIdWithLock(anyLong());
    }

    @Test
    @DisplayName("Should reject cash checkout when tendered amount is less than total")
    void shouldRejectUnderpaidCash() {
        CheckoutRequest request = CheckoutRequest.builder()
                .items(List.of(new CartItemRequest(10L, 1)))
                .paymentMethod("CASH")
                .cashTendered(new BigDecimal("5.00")) // Total is $10.80 ($10 + 8% tax)
                .discountAmount(BigDecimal.ZERO)
                .build();

        when(userRepository.findByUsername("cashier")).thenReturn(Optional.of(cashier));
        when(productRepository.findById(10L)).thenReturn(Optional.of(espresso));
        when(inventoryRepository.findByProductIdWithLock(10L)).thenReturn(Optional.of(inventory));

        assertThrows(BadRequestException.class, () -> {
            checkoutService.processCheckout(request, "cashier");
        });

        verify(saleRepository, never()).save(any(Sale.class));
    }
}
