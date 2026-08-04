package com.chijia.erp.model.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import lombok.Data;

@Data
public class PurchaseOrderDTO {
	private Long id;
	private String purchaseNo;
	private Long supplierId;
	private BigDecimal totalAmount;
	private LocalDateTime purchaseDate;
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
