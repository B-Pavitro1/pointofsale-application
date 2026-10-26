package com.pos.service;

import com.pos.dto.product.ProductRequest;
import com.pos.dto.product.ProductResponse;
import com.pos.entity.Inventory;
import com.pos.entity.Product;
import com.pos.exception.BadRequestException;
import com.pos.exception.ResourceNotFoundException;
import com.pos.repository.InventoryRepository;
import com.pos.repository.ProductRepository;
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
public class ProductService {

    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;

    @Transactional(readOnly = true)
    public List<ProductResponse> getAllActiveProducts() {
        return productRepository.findByIsActiveTrue().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ProductResponse getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        return mapToResponse(product);
    }

    @Transactional(readOnly = true)
    public ProductResponse getProductByBarcode(String barcode) {
        Product product = productRepository.findByBarcode(barcode)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with barcode: " + barcode));
        return mapToResponse(product);
    }

    @Transactional
    public ProductResponse createProduct(ProductRequest request) {
        log.info("Creating new product with SKU: {}", request.getSku());

        if (productRepository.existsBySku(request.getSku())) {
            throw new BadRequestException("Product with SKU already exists: " + request.getSku());
        }
        if (request.getBarcode() != null && !request.getBarcode().isBlank()
                && productRepository.existsByBarcode(request.getBarcode())) {
            throw new BadRequestException("Product with Barcode already exists: " + request.getBarcode());
        }

        Product product = Product.builder()
                .sku(request.getSku().toUpperCase().trim())
                .barcode(request.getBarcode() != null ? request.getBarcode().trim() : null)
                .name(request.getName().trim())
                .category(request.getCategory().trim())
                .price(request.getPrice())
                .costPrice(request.getCostPrice())
                .isActive(true)
                .build();

        Product savedProduct = productRepository.save(product);

        Inventory inventory = Inventory.builder()
                .product(savedProduct)
                .quantity(request.getInitialQuantity() != null ? request.getInitialQuantity() : 0)
                .lowStockThreshold(request.getLowStockThreshold() != null ? request.getLowStockThreshold() : 10)
                .lastRestockedAt(LocalDateTime.now())
                .build();

        inventoryRepository.save(inventory);
        savedProduct.setInventory(inventory);

        return mapToResponse(savedProduct);
    }

    @Transactional
    public ProductResponse updateProduct(Long id, ProductRequest request) {
        log.info("Updating product id: {}", id);

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));

        // Check if SKU changed and already taken
        if (!product.getSku().equalsIgnoreCase(request.getSku()) && productRepository.existsBySku(request.getSku())) {
            throw new BadRequestException("Product with SKU already exists: " + request.getSku());
        }

        product.setSku(request.getSku().toUpperCase().trim());
        product.setBarcode(request.getBarcode() != null ? request.getBarcode().trim() : null);
        product.setName(request.getName().trim());
        product.setCategory(request.getCategory().trim());
        product.setPrice(request.getPrice());
        product.setCostPrice(request.getCostPrice());

        if (product.getInventory() != null && request.getLowStockThreshold() != null) {
            product.getInventory().setLowStockThreshold(request.getLowStockThreshold());
        }

        Product updated = productRepository.save(product);
        return mapToResponse(updated);
    }

    @Transactional
    public void deleteProduct(Long id) {
        log.info("Soft-deleting product id: {}", id);
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        product.setIsActive(false);
        productRepository.save(product);
    }

    public ProductResponse mapToResponse(Product product) {
        Inventory inv = product.getInventory();
        Integer quantity = (inv != null) ? inv.getQuantity() : 0;
        Integer threshold = (inv != null) ? inv.getLowStockThreshold() : 10;
        boolean lowStock = (inv != null) && inv.isLowStock();

        return ProductResponse.builder()
                .id(product.getId())
                .sku(product.getSku())
                .barcode(product.getBarcode())
                .name(product.getName())
                .category(product.getCategory())
                .price(product.getPrice())
                .costPrice(product.getCostPrice())
                .quantity(quantity)
                .lowStockThreshold(threshold)
                .lowStock(lowStock)
                .isActive(product.getIsActive())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }
}
