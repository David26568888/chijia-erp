package com.chijia.erp.model.dto;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
//(前端開單請求)
@Data
public class CreateSaleOrderDTO {
	private Long customerId; //客戶ID
	private String remark;// 銷貨備註
	
	@NotEmpty(message = "銷貨明細不能為空")
	private List<CreateItemDTO> items;// 開單商品明細列表
	
	@Data
	public static class CreateItemDTO{
		
		@NotNull(message = "商品 ID 不能為空")
		private Long productId; // 商品ID
		
		@NotNull(message = "銷貨數量不能為空")
        private BigDecimal quantity; // 💡 修改為 BigDecimal (支援 1.5 尺等小數銷貨)

        // 銷售單價 (選填)：若未帶入，Service 會自動帶入商品設定的預設零售價 (salePrice)
        private BigDecimal unitPrice;
	}
}
