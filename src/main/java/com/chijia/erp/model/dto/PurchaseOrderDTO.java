package com.chijia.erp.model.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import lombok.Data;

@Data
public class PurchaseOrderDTO {
    private Long id;
    private String purchaseNo;
    private Long supplierId;
    private String supplierName; // 💡 廠商名稱 (方便前端表格顯示)
    private BigDecimal totalAmount;
    private BigDecimal discountAmount;
    private LocalDate purchaseDate;
    private String remark;
    private List<ItemDTO> items;

    @Data
    public static class ItemDTO {
        private Long id;
        private Long productId;
        private String productCode;
        private String productName;
        private BigDecimal quantity;
        private BigDecimal unitPrice;
        private BigDecimal subtotal;
    }
}