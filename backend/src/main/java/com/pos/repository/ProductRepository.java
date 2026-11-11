package com.pos.repository;

import com.pos.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    Optional<Product> findBySku(String sku);
    Optional<Product> findByBarcode(String barcode);
    List<Product> findByIsActiveTrue();

    @Query("SELECT p FROM Product p LEFT JOIN FETCH p.inventory WHERE p.isActive = true")
    List<Product> findAllActiveWithInventory();

    List<Product> findByCategoryIgnoreCaseAndIsActiveTrue(String category);
    boolean existsBySku(String sku);
    boolean existsByBarcode(String barcode);
}
