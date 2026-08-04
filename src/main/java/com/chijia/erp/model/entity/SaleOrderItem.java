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
	
	// 💡銷貨數量 (改為 BigDecimal，支援 1.5 尺、0.8 公斤等銷貨)
    @Column(name = "quantity", nullable = false, precision = 12, scale = 2)
    private BigDecimal quantity = BigDecimal.ZERO;
	
    // 💡銷售單價 (改為 BigDecimal)
    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice = BigDecimal.ZERO;

    // 💡小計金額 (改為 BigDecimal)
    @Column(name = "subtotal", nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal = BigDecimal.ZERO;
}
