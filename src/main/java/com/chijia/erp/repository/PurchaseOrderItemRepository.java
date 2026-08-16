package com.chijia.erp.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.chijia.erp.model.entity.PurchaseOrderItem;

@Repository
public interface PurchaseOrderItemRepository extends JpaRepository<PurchaseOrderItem, Long> {

    // 💡 查詢商品最新進貨歷史紀錄（簡化方法名，透過 JPQL 排序）
    @Query("SELECT item FROM PurchaseOrderItem item WHERE item.productId = :productId ORDER BY item.purchaseOrder.purchaseDate DESC, item.purchaseOrder.id DESC")
    List<PurchaseOrderItem> findRecentHistoryByProductId(@Param("productId") Long productId, Pageable pageable);
}