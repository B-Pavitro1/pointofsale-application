package com.pos.service;

import com.pos.dto.product.ProductResponse;
import com.pos.dto.product.StockAdjustmentRequest;
import com.pos.entity.Inventory;
import com.pos.exception.BadRequestException;
import com.pos.exception.ResourceNotFoundException;
import com.pos.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final ProductService productService;

    @Transactional(readOnly = true)
    public List<ProductResponse> getLowStockAlerts() {
        return inventoryRepository.findLowStockItems().stream()
                .map(inv -> productService.mapToResponse(inv.getProduct()))
                .collect(Collectors.toList());
    }

    @Transactional
    public ProductResponse adjustStock(Long productId, StockAdjustmentRequest request) {
        log.info("Adjusting stock for product {} with operation {} by amount {}",
                productId, request.getOperation(), request.getQuantity());

        Inventory inventory = inventoryRepository.findByProductIdWithLock(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory not found for product id: " + productId));

        if ("ADD".equalsIgnoreCase(request.getOperation())) {
            inventory.setQuantity(inventory.getQuantity() + request.getQuantity());
            inventory.setLastRestockedAt(LocalDateTime.now());
        } else if ("SET".equalsIgnoreCase(request.getOperation())) {
            if (request.getQuantity() < 0) {
                throw new BadRequestException("Inventory quantity cannot be negative");
            }
            inventory.setQuantity(request.getQuantity());
            inventory.setLastRestockedAt(LocalDateTime.now());
        } else {
            throw new BadRequestException("Unsupported operation: " + request.getOperation() + ". Use ADD or SET.");
        }

        Inventory updated = inventoryRepository.save(inventory);
        return productService.mapToResponse(updated.getProduct());
    }
}
