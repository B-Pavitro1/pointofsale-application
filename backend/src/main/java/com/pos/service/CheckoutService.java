package com.pos.service;

import com.pos.dto.sale.CartItemRequest;
import com.pos.dto.sale.CheckoutRequest;
import com.pos.dto.sale.CheckoutResponse;
import com.pos.dto.sale.SaleItemResponse;
import com.pos.entity.*;
import com.pos.exception.BadRequestException;
import com.pos.exception.InsufficientStockException;
import com.pos.exception.ResourceNotFoundException;
import com.pos.repository.InventoryRepository;
import com.pos.repository.ProductRepository;
import com.pos.repository.SaleRepository;
import com.pos.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CheckoutService {

    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final SaleRepository saleRepository;
    private final UserRepository userRepository;
    private final PaymentService paymentService;

    private static final BigDecimal TAX_RATE = new BigDecimal("0.08"); // 8% sales tax

    /**
     * Executes the checkout transaction with pessimistic locking on inventory rows.
     * Prevents race conditions and overselling across concurrent checkout threads.
     */
    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public CheckoutResponse processCheckout(CheckoutRequest request, String cashierUsername) {
        log.info("Initiating checkout by cashier: {} with {} items", cashierUsername, request.getItems().size());

        User cashier = userRepository.findByUsername(cashierUsername)
                .orElseThrow(() -> new ResourceNotFoundException("Cashier user not found: " + cashierUsername));

        BigDecimal subtotal = BigDecimal.ZERO;
        List<SaleItem> saleItems = new ArrayList<>();
        List<SaleItemResponse> itemResponses = new ArrayList<>();

        // Process and lock each inventory item sequentially
        for (CartItemRequest itemReq : request.getItems()) {
            Product product = productRepository.findById(itemReq.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + itemReq.getProductId()));

            if (!Boolean.TRUE.equals(product.getIsActive())) {
                throw new BadRequestException("Cannot purchase inactive product: " + product.getName());
            }

            // Pessimistic write lock on inventory row
            Inventory inventory = inventoryRepository.findByProductIdWithLock(product.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Inventory record not found for product: " + product.getName()));

            if (inventory.getQuantity() < itemReq.getQuantity()) {
                throw new InsufficientStockException(String.format(
                        "Insufficient stock for '%s' (SKU: %s). Requested: %d, Available: %d",
                        product.getName(), product.getSku(), itemReq.getQuantity(), inventory.getQuantity()
                ));
            }

            // Deduct stock atomically within transaction
            inventory.setQuantity(inventory.getQuantity() - itemReq.getQuantity());
            inventoryRepository.save(inventory);

            BigDecimal lineTotal = product.getPrice().multiply(BigDecimal.valueOf(itemReq.getQuantity()));
            subtotal = subtotal.add(lineTotal);

            SaleItem saleItem = SaleItem.builder()
                    .product(product)
                    .quantity(itemReq.getQuantity())
                    .unitPrice(product.getPrice())
                    .subtotal(lineTotal)
                    .build();
            saleItems.add(saleItem);

            itemResponses.add(SaleItemResponse.builder()
                    .productId(product.getId())
                    .productSku(product.getSku())
                    .productName(product.getName())
                    .quantity(itemReq.getQuantity())
                    .unitPrice(product.getPrice())
                    .subtotal(lineTotal)
                    .build());
        }

        BigDecimal discount = request.getDiscountAmount() != null ? request.getDiscountAmount() : BigDecimal.ZERO;
        if (discount.compareTo(subtotal) > 0) {
            throw new BadRequestException("Discount amount cannot exceed cart subtotal");
        }

        BigDecimal taxableAmount = subtotal.subtract(discount);
        BigDecimal taxAmount = taxableAmount.multiply(TAX_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalAmount = taxableAmount.add(taxAmount).setScale(2, RoundingMode.HALF_UP);

        BigDecimal cashTendered = request.getCashTendered();
        BigDecimal changeDue = BigDecimal.ZERO;
        if ("CASH".equalsIgnoreCase(request.getPaymentMethod()) && cashTendered != null) {
            if (cashTendered.compareTo(totalAmount) < 0) {
                throw new BadRequestException(String.format("Cash tendered (%s) is less than total amount (%s)", cashTendered, totalAmount));
            }
            changeDue = cashTendered.subtract(totalAmount).setScale(2, RoundingMode.HALF_UP);
        }

        // Authorize payment via Mock Payment Service
        String txnRef = paymentService.processPayment(request.getPaymentMethod(), totalAmount);

        // Generate unique invoice number: INV-YYYYMMDD-XXXX
        String datePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        int randomSuffix = ThreadLocalRandom.current().nextInt(1000, 9999);
        String invoiceNumber = "INV-" + datePart + "-" + randomSuffix;

        Sale sale = Sale.builder()
                .invoiceNumber(invoiceNumber)
                .cashier(cashier)
                .subtotal(subtotal)
                .taxAmount(taxAmount)
                .discountAmount(discount)
                .totalAmount(totalAmount)
                .status("COMPLETED")
                .build();

        for (SaleItem item : saleItems) {
            sale.addItem(item);
        }

        Payment payment = Payment.builder()
                .paymentMethod(request.getPaymentMethod().toUpperCase())
                .amount(totalAmount)
                .status("SUCCESS")
                .transactionReference(txnRef)
                .build();
        sale.addPayment(payment);

        Sale savedSale = saleRepository.save(sale);
        log.info("Checkout completed successfully. Invoice: {}", invoiceNumber);

        return CheckoutResponse.builder()
                .saleId(savedSale.getId())
                .invoiceNumber(savedSale.getInvoiceNumber())
                .cashierName(cashier.getFullName())
                .subtotal(subtotal)
                .taxAmount(taxAmount)
                .discountAmount(discount)
                .totalAmount(totalAmount)
                .status(savedSale.getStatus())
                .paymentMethod(request.getPaymentMethod().toUpperCase())
                .transactionReference(txnRef)
                .cashTendered(cashTendered)
                .changeDue(changeDue)
                .items(itemResponses)
                .createdAt(savedSale.getCreatedAt())
                .build();
    }

    @Transactional(readOnly = true)
    public List<CheckoutResponse> getRecentSales() {
        return saleRepository.findTop50ByOrderByCreatedAtDesc().stream()
                .map(this::mapSaleToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CheckoutResponse getSaleByInvoice(String invoiceNumber) {
        Sale sale = saleRepository.findByInvoiceNumber(invoiceNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Sale not found with invoice: " + invoiceNumber));
        return mapSaleToResponse(sale);
    }

    private CheckoutResponse mapSaleToResponse(Sale sale) {
        List<SaleItemResponse> itemResponses = sale.getItems().stream()
                .map(i -> SaleItemResponse.builder()
                        .productId(i.getProduct().getId())
                        .productSku(i.getProduct().getSku())
                        .productName(i.getProduct().getName())
                        .quantity(i.getQuantity())
                        .unitPrice(i.getUnitPrice())
                        .subtotal(i.getSubtotal())
                        .build())
                .collect(Collectors.toList());

        String paymentMethod = "CASH";
        String txnRef = "N/A";
        if (!sale.getPayments().isEmpty()) {
            Payment p = sale.getPayments().get(0);
            paymentMethod = p.getPaymentMethod();
            txnRef = p.getTransactionReference();
        }

        return CheckoutResponse.builder()
                .saleId(sale.getId())
                .invoiceNumber(sale.getInvoiceNumber())
                .cashierName(sale.getCashier() != null ? sale.getCashier().getFullName() : "Unknown")
                .subtotal(sale.getSubtotal())
                .taxAmount(sale.getTaxAmount())
                .discountAmount(sale.getDiscountAmount())
                .totalAmount(sale.getTotalAmount())
                .status(sale.getStatus())
                .paymentMethod(paymentMethod)
                .transactionReference(txnRef)
                .items(itemResponses)
                .createdAt(sale.getCreatedAt())
                .build();
    }
}
