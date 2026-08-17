package com.chijia.erp.service;

import com.chijia.erp.model.dto.ProductHistoryDTO;

public interface ProductHistoryService {
    
    /**
     * 取得指定商品的歷史行情 (包含前 10 筆銷售與前 10 筆進貨紀錄)
     */
    ProductHistoryDTO getProductHistory(Long productId);
}