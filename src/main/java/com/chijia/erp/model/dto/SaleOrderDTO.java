package com.chijia.erp.model.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import lombok.Data;

//(後端回傳結果)
@Data
public class SaleOrderDTO {
	private Long id;
	private String saleNo;
	private Long customerId;
	private BigDecimal totalAmount;
	private LocalDateTime saleDate;
	private String remark;
	private List<ItemDTO> items;
	
	@Data
	public static class ItemDTO{
		private Long id;
		private Long productId;
		private String productName;
		private BigDecimal quantity;
		private BigDecimal unitPrice;
		private BigDecimal subtotal;
		
	}
}
