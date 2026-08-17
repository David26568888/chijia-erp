package com.chijia.erp.repository;

import java.awt.print.Pageable;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.chijia.erp.model.entity.SaleOrder;

@Repository
public interface SaleOrderRepository extends JpaRepository<SaleOrder, Long> {

    // 檢查銷貨單號是否重複
    boolean existsBySaleNo(String saleNo);

    Optional<SaleOrder> findBySaleNo(String saleNo);

    // 1. 查詢指定月份的總營業額
    @Query("SELECT SUM(o.totalAmount) FROM SaleOrder o WHERE YEAR(o.saleDate) = :year AND MONTH(o.saleDate) = :month")
    BigDecimal sumTotalAmountByMonth(@Param("year") int year, @Param("month") int month);

    // 2. 查詢指定月份的總毛利
    @Query("SELECT SUM(i.grossProfit) FROM SaleOrder o JOIN o.items i WHERE YEAR(o.saleDate) = :year AND MONTH(o.saleDate) = :month")
    BigDecimal sumGrossProfitByMonth(@Param("year") int year, @Param("month") int month);

    // 3. 修正 JPQL：跨表關聯到 customer.id，查詢客戶歷史成交價
    @Query("SELECT i.unitPrice FROM SaleOrder o JOIN o.items i WHERE o.customer.id = :customerId AND i.productId = :productId ORDER BY o.saleDate DESC, o.id DESC")
    List<BigDecimal> findRecentPriceByCustomerAndProduct(@Param("customerId") Long customerId, @Param("productId") Long productId);
    

}