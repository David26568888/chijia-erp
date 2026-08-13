package com.chijia.erp.model.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreatePurchaseOrderDTO {

    @NotNull(message = "廠商 ID 不可為空！")
    private Long supplierId;

    private LocalDate purchaseDate; // 進貨日期

    private String remark; // 備註

    private BigDecimal discountAmount = BigDecimal.ZERO; // 整單折讓

    @NotEmpty(message = "進貨單必須包含至少一項商品！")
    @Valid
    private List<CreateItemDTO> items;

    @Data
    public static class CreateItemDTO {

        @NotNull(message = "商品 ID 不可為空！")
        private Long productId;

        @NotNull(message = "進貨數量不可為空！")
        private BigDecimal quantity;

        // 進貨單價 (若未填，自動帶入商品預設進貨成本)
        private BigDecimal unitPrice;
    }
}