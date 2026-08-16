package com.chijia.erp.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.chijia.erp.model.entity.SaleOrderItem;

@Repository
public interface SaleOrderItemRepository extends JpaRepository<SaleOrderItem, Long> {

    // 💡 1. 查詢商品最新銷貨歷史紀錄
    @Query("SELECT item FROM SaleOrderItem item WHERE item.productId = :productId ORDER BY item.saleOrder.saleDate DESC, item.saleOrder.id DESC")
    List<SaleOrderItem> findRecentHistoryByProductId(@Param("productId") Long productId, Pageable pageable);

    // 💡 2. 查詢該客戶購買該商品的最新一筆交易紀錄 (帶入上次成交價)
    @Query("SELECT item FROM SaleOrderItem item WHERE item.saleOrder.customer.id = :customerId AND item.productId = :productId ORDER BY item.saleOrder.saleDate DESC, item.saleOrder.id DESC")
    List<SaleOrderItem> findRecentPriceByCustomerAndProduct(@Param("customerId") Long customerId, @Param("productId") Long productId, Pageable pageable);
}