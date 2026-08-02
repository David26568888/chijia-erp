package com.chijia.erp.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.chijia.erp.model.entity.PurchaseOrder;
import java.util.List;
import java.util.Optional;


public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder,Long> {

	// 💡 透過進貨單號 (purchaseNo) 查詢單據
	Optional<PurchaseOrder>  findByPurchaseNo(String purchaseNo);
}
