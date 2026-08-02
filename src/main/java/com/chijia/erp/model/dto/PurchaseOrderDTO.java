package com.chijia.erp.model.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.chijia.erp.model.dto.SaleOrderDTO.ItemDTO;

import lombok.Data;

@Data
public class PurchaseOrderDTO {
	private Long id;
	private String purchaseNo;
	private Long supplierId;
	private Integer totalAmount;
	private LocalDateTime purchaseDate;
	private String remark;
	private List<ItemDTO> items;
	
	@Data
	public static class ItemDTO{
		private Long id;
		private Long productId;
		private String productName;
		private Integer quantity;
		private Integer unitPrice;
		private Integer subtotal;
		
	}
}
