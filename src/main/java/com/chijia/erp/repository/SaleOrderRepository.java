package com.chijia.erp.repository;

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
	
	//檢查單號是否重覆
	boolean existsBySaleNo(String saleNo);
	
	Optional<SaleOrder> findBySaleNo(String saleNo);
	
	// 1. 查詢指定月份的總營業額
    @Query("SELECT SUM(o.totalAmount) FROM SaleOrder o WHERE YEAR(o.saleDate) = :year AND MONTH(o.saleDate) = :month")
    BigDecimal sumTotalAmountByMonth(@Param("year") int year, @Param("month") int month);

    // 2. 查詢指定月份的總毛利 (加總明細表中的 grossProfit)
    @Query("SELECT SUM(i.grossProfit) FROM SaleOrder o JOIN o.items i WHERE YEAR(o.saleDate) = :year AND MONTH(o.saleDate) = :month")
    BigDecimal sumGrossProfitByMonth(@Param("year") int year, @Param("month") int month);

    // 3. 查詢最賺錢的商品排行榜 (依總毛利排序)
    // 回傳格式: [productName, totalProfit, totalQty]
    @Query("SELECT i.productName, SUM(i.grossProfit) as totalProfit, SUM(i.quantity) as totalQty " +
           "FROM SaleOrderItem i GROUP BY i.productCode, i.productName ORDER BY totalProfit DESC")
    List<Object[]> findTopProfitableProducts();

    // 4. 查詢銷量最好的商品排行榜 (依銷售總數量排序)
    @Query("SELECT i.productName, SUM(i.quantity) as totalQty, SUM(i.subtotal) as totalSales " +
           "FROM SaleOrderItem i GROUP BY i.productCode, i.productName ORDER BY totalQty DESC")
    List<Object[]> findTopSellingProducts();
}
