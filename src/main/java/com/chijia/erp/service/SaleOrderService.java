package com.chijia.erp.service;

import com.chijia.erp.model.dto.CreateSaleOrderDTO;
import com.chijia.erp.model.dto.SaleOrderDTO;

//撰寫核心 Service（交易與扣減庫存）
public interface SaleOrderService {
	// 建立銷貨單並扣減庫存
	SaleOrderDTO createSaleOrder(CreateSaleOrderDTO createDTO);
	
	// 依據 ID 查詢銷貨單
	SaleOrderDTO getSaleOrderById(Long id);
}
