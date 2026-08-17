package com.chijia.erp.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.chijia.erp.model.entity.PurchaseOrderItem;
import com.chijia.erp.model.entity.SaleOrderItem;

@Repository
public interface SaleOrderItemRepository extends JpaRepository<SaleOrderItem, Long> {

    // 💡 1. 查詢商品最新銷貨歷史紀錄
    @Query("SELECT item FROM SaleOrderItem item WHERE item.productId = :productId ORDER BY item.saleOrder.saleDate DESC, item.saleOrder.id DESC")
    List<SaleOrderItem> findRecentHistoryByProductId(@Param("productId") Long productId, Pageable pageable);

    // 💡 2. 查詢該客戶購買該商品的最新一筆交易紀錄 (帶入上次成交價)
    @Query("SELECT item FROM SaleOrderItem item WHERE item.saleOrder.customer.id = :customerId AND item.productId = :productId ORDER BY item.saleOrder.saleDate DESC, item.saleOrder.id DESC")
    List<SaleOrderItem> findRecentPriceByCustomerAndProduct(@Param("customerId") Long customerId, @Param("productId") Long productId, Pageable pageable);
    
    // 💡 3. 最賺錢商品排行榜 (按總毛利加總排序)
    @Query("SELECT i.productId, i.productName, SUM(i.grossProfit) AS totalProfit " +
           "FROM SaleOrderItem i " +
           "GROUP BY i.productId, i.productName " +
           "ORDER BY totalProfit DESC")
    List<Object[]> findTopProfitableProducts(Pageable pageable);

    // 💡 4. 熱銷商品排行榜 (按銷售總數量加總排序)
    @Query("SELECT i.productId, i.productName, SUM(i.quantity) AS totalQty " +
           "FROM SaleOrderItem i " +
           "GROUP BY i.productId, i.productName " +
           "ORDER BY totalQty DESC")
    List<Object[]> findTopSellingProducts(Pageable pageable);
    
    //5.查詢最近10筆進貨銷售紀錄
    List<SaleOrderItem> findTop10ByProductIdOrderBySaleOrderSaleDateDesc(Long productId);
}