package com.chijia.erp.model.entity;

import org.hibernate.annotations.ManyToAny;

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
@Table(name="purchase_order_item")
public class PurchaseOrderItem {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	// 💡 多對一關聯回進貨單主檔
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="purchase_order_id",nullable = false)
	private PurchaseOrder purchaseOrder;
	
	@Column(name = "product_id",nullable = false)
	private Long productId;// 進貨商品ID
	
	@Column(name = "product_name")
	private String productName;// 備份商品名稱 (避免商品未來改名影響歷史進貨紀錄)
	
	@Column(nullable = false)
	private Integer quantity;// 進貨數量
	
	@Column(name = "unit_price", nullable = false)
	private Integer unitPrice;// 進貨單價 (進價/成本)
	
	@Column(nullable = false)
	private Integer subtotal;// 小計金額 (quantity * unitPrice)
}
