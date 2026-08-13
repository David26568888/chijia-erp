package com.chijia.erp.service;

import java.time.LocalDate;
import java.util.List;

import com.chijia.erp.model.dto.CreatePurchaseOrderDTO;
import com.chijia.erp.model.dto.PurchaseOrderDTO;

public interface PurchaseOrderService {

    // 1. 查詢所有進貨單列表 (解決前端 No data 的核心 API)
    List<PurchaseOrderDTO> getAllPurchaseOrders();

    // 2. 條件過濾查詢 (依關鍵字或日期區間)
    List<PurchaseOrderDTO> searchPurchaseOrders(String keyword, LocalDate startDate, LocalDate endDate);

    // 3. 依據 ID 查詢單一進貨單與明細[cite: 17]
    PurchaseOrderDTO getPurchaseOrderById(Long id);

    // 4. 建立進貨單並自動增加庫存與更新三軌成本[cite: 17]
    PurchaseOrderDTO createPurchaseOrder(CreatePurchaseOrderDTO createDTO);

    // 5. 修改進貨單 (自動校正庫存與成本)
    PurchaseOrderDTO updatePurchaseOrder(Long id, CreatePurchaseOrderDTO updateDTO);

    // 6. 作廢/刪除進貨單 (自動將當時增加的庫存扣回)
    void deletePurchaseOrder(Long id);
}