package com.chijia.erp.model.dto;

import java.math.BigDecimal;
import java.util.List;

import lombok.Data;
//(前端開單請求)
@Data
public class CreateSaleOrderDTO {
	private Long customerId; //客戶ID
	private String remark;// 銷貨備註
	private List<CreateItemDTO> items;// 開單商品明細列表
	
	@Data
	public static class CreateItemDTO{
		private Long productId; // 商品ID
		private Integer quantity; // 購買數量
	}
}
