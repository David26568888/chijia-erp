package com.chijia.erp.model.dto;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import lombok.Data;

@Data
public class CreatePurchaseOrderDTO {
	
	@NotNull(message = "廠商 ID 不能為空")
	private Long supplierId;
	
	private String remark;// 進貨備註 (選填)
	
	@NotEmpty(message = "進貨明細不能為空")
	private List<CreateItemDTO> items;
	
	@Data
	public static class CreateItemDTO{
		
		@NotNull(message = "商品 ID 不能為空")
		private Long productId;
		
		@NotNull(message = "進貨數量不能為空")
		private Integer quantity;
		
		// 💡 進貨單價 (選填)：若採購有手動輸入實際批發價就以此為主；若未填，Service 會自動抓取商品的預設進價 (costPrice)
		private Integer unitPrice;
		
	}
	
}
