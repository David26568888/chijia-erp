package com.chijia.erp.model.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import lombok.Data;

@Data
public class SaleOrderDTO {
    private Long id;
    private String saleNo;
    private Long customerId;
    private String customerName; // 💡 客戶名稱 (方便前端顯示)
    private BigDecimal totalAmount; // 實收總金額
    private BigDecimal discountAmount; // 💡 整單折讓金額
    private LocalDate saleDate;
    private String remark;
    private List<ItemDTO> items;

    @Data
    public static class ItemDTO {
        private Long id;
        private Long productId;
        private String productCode; // 💡 商品編號
        private String productName; // 商品名稱
        private BigDecimal quantity;
        private BigDecimal unitPrice;
        private BigDecimal subtotal;
    }
}