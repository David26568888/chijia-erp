package com.chijia.erp.model.dto;

import java.math.BigDecimal;
import lombok.Data;

@Data // Lombok 會自動生成 Getter/Setter/toString
public class ProductDTO {
    private Long id;
    private String productCode;   // 產品編號
    private String productName;   // 品名規格
    private String barcode;       // 條碼編號
    private String unit;          // 單位
    private BigDecimal salePrice; // 售價
    // 實務設計：為了防止利潤外洩，我們在常規的 DTO 中不對前端暴露 costPrice (進價)
    private BigDecimal stockQuantity; // 👈 必須補上
    private BigDecimal safetyStock;   // 👈 必須補上
    private boolean status;       // 狀態
}