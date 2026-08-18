package com.chijia.erp.service;

import org.springframework.web.multipart.MultipartFile;

public interface ExcelImportService {
 
    /**
     * 匯入歷史進貨紀錄 (支援舊 ERP 進貨日報明細表格式)
     * @param file 上傳的 Excel 檔案
     * @return 成功匯入的單數訊息
     */
    String importPurchaseOrdersFromExcel(MultipartFile file);

	String importSaleOrdersFromExcel(MultipartFile file, boolean deductStock);
}