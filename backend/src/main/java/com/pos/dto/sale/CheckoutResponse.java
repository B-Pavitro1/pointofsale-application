package com.pos.dto.sale;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CheckoutResponse {

    private Long saleId;
    private String invoiceNumber;
    private String cashierName;
    private BigDecimal subtotal;
    private BigDecimal taxAmount;
    private BigDecimal discountAmount;
    private BigDecimal totalAmount;
    private String status;
    private String paymentMethod;
    private String transactionReference;
    private BigDecimal cashTendered;
    private BigDecimal changeDue;
    private List<SaleItemResponse> items;
    private LocalDateTime createdAt;
}
