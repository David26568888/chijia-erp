package com.chijia.erp.service;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.chijia.erp.model.dto.CreateSaleOrderDTO;
import com.chijia.erp.model.dto.SaleOrderDTO;

public interface SaleOrderService {
    // 1. 建立銷貨單並扣減庫存[cite: 9]
    SaleOrderDTO createSaleOrder(CreateSaleOrderDTO createDTO);
	
    // 2. 依據 ID 查詢銷貨單[cite: 9]
    SaleOrderDTO getSaleOrderById(Long id);

    // 3. 查詢所有銷貨單列表
    List<SaleOrderDTO> getAllSaleOrders();

    // 4. 條件過濾搜尋銷貨單
    List<SaleOrderDTO> searchSaleOrders(String keyword, LocalDate startDate, LocalDate endDate);

    // 5. 修改銷貨單
    SaleOrderDTO updateSaleOrder(Long id, CreateSaleOrderDTO updateDTO);

    // 💡 6. 核心作廢功能：刪除/作廢銷貨單 (並自動將商品庫存加回)
    void deleteSaleOrder(Long id);
    
 // 💡7.查詢建議售價 (歷史價格記憶)
    BigDecimal getSuggestedPrice(Long customerId, Long productId);
    
 // 8.匯入 Excel 歷史紀錄
    String importSaleOrdersFromExcel(MultipartFile file, boolean deductStock);

    // 9. 匯出 Excel 報表
    byte[] exportSaleOrdersToExcel() throws IOException;
}