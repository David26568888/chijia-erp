package com.chijia.erp.model.entity;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name ="sale_order_item")
public class SaleOrderItem {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	//💡 多對一關聯回主檔
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "sale_order_id", nullable = false)
	private SaleOrder saleOrder;
	
	@Column(name = "product_id" , nullable = false)
	private Long productId;// 商品ID
	
	@Column(name = "product_name")
	private String productName; // 備份商品名稱 (避免商品改名後歷史數據跑掉)
	
	@Column(nullable = false)
	private Integer quantity;// 銷售數量
	
	@Column(name = "unit_price" , nullable = false)
	private BigDecimal unitPrice; // 銷售單價

	@Column(nullable = false)
	private BigDecimal subtotal; // 小計金額 (quantity * unitPrice)
}
