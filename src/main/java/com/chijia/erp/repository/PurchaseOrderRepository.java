package com.chijia.erp.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.chijia.erp.model.entity.PurchaseOrder;

@Repository
public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {

    // 💡 檢查進貨單號是否存在
    boolean existsByPurchaseNo(String purchaseNo);

    // 💡 透過進貨單號 (purchaseNo) 查詢單據
    Optional<PurchaseOrder> findByPurchaseNo(String purchaseNo);
}