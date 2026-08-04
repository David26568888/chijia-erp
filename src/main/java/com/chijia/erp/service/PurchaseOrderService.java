package com.chijia.erp.service;

import org.springframework.stereotype.Service;

import com.chijia.erp.model.dto.CreatePurchaseOrderDTO;
import com.chijia.erp.model.dto.PurchaseOrderDTO;


public interface PurchaseOrderService {
	
	// 💡 建立進貨單並自動增加庫存 (補貨)
	PurchaseOrderDTO createPurchaseOrder(CreatePurchaseOrderDTO createDTO);
	
	// 💡 依據 ID 查詢單一進貨單與明細
	PurchaseOrderDTO getPurchaseOrderById(Long id);
}
