package com.pos.repository;

import com.pos.entity.Sale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SaleRepository extends JpaRepository<Sale, Long> {

    Optional<Sale> findByInvoiceNumber(String invoiceNumber);

    List<Sale> findByCreatedAtBetweenOrderByCreatedAtDesc(LocalDateTime start, LocalDateTime end);

    @Query("SELECT COUNT(s) FROM Sale s WHERE s.status = 'COMPLETED'")
    long countCompletedSales();

    @Query("SELECT COALESCE(SUM(s.totalAmount), 0) FROM Sale s WHERE s.status = 'COMPLETED'")
    BigDecimal sumTotalRevenue();

    @Query("SELECT COALESCE(SUM(s.totalAmount), 0) FROM Sale s WHERE s.status = 'COMPLETED' AND s.createdAt BETWEEN :start AND :end")
    BigDecimal sumTotalRevenueBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    List<Sale> findTop50ByOrderByCreatedAtDesc();
}
