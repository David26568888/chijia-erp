package com.chijia.erp.model.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateSaleOrderDTO {

    // 客戶 ID (允許為 null 代表門市散客結帳)
    private Long customerId;

    // 💡 銷貨日期 (預設為今天，支援選擇過往日期補開單)
    private LocalDate saleDate;

    // 銷貨備註
    private String remark;

    // 💡 整單折讓 / 折扣金額 (例如：去零頭 50 元)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    // 銷貨商品明細列表 (至少需包含一項商品)
    @NotEmpty(message = "銷貨單必須包含至少一項商品！")
    @Valid
    private List<CreateItemDTO> items;

    @Data
    public static class CreateItemDTO {

        @NotNull(message = "商品 ID 不可為空！")
        private Long productId;

        @NotNull(message = "銷售數量不可為空！")
        private BigDecimal quantity;

        // 銷售單價 (若未填寫，後端會自動帶入商品設定的預設零售價 salePrice)
        private BigDecimal unitPrice;
    }
}