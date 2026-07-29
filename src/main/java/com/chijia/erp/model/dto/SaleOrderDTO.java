package com.chijia.erp.model.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import lombok.Data;

//(後端回傳結果)
@Data
public class SaleOrderDTO {
	private Long id;
	private String orderNo;
	private Long customerId;
	private BigDecimal totalAmount;
	private LocalDateTime orderDate;
	private String remark;
	private List<ItemDTO> items;
	
	@Data
	public static class ItemDTO{
		private Long id;
		private Long productId;
		private String productName;
		private Integer quantity;
		private BigDecimal unitPrice;
		private BigDecimal subtotal;
		
	}
}
