package com.chijia.erp.model.dto;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class ProductDTO {
    private Long id;
    private String productCode;   // 商品編號
    private String productName;   // 品名規格
    private String barcode;       // 條碼
    private String unit;          // 單位 (如：個、包、盒、尺)
    private BigDecimal salePrice; // 零售售價

    // 💡 三軌成本欄位
    private BigDecimal costPrice;     // 1. 預設 / 基準成本 (建檔預設)
    private BigDecimal lastCostPrice; // 2. 最後進價 (每次進貨單建立時直接覆蓋)
    private BigDecimal avgCostPrice;  // 3. 移動加權平均成本 (每次進貨單建立時按數量金額加權重算)

    private BigDecimal stockQuantity; // 現有庫存數量
    private BigDecimal safetyStock;   // 安全存量
    private boolean status;           // 狀態 (true: 啟用, false: 停用)
}