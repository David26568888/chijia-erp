package com.chijia.erp.model.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "sale_order")
public class SaleOrder {

	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	@Column(name = "order_no", nullable = false, unique = true, length = 30)
	private String orderNo;// 銷貨單號 (例如: SO-20260729-001)
	
	@Column(name = "customer_id",nullable = false)
	private Long customerId;// 客戶ID
	
	@Column(name = "total_amount", nullable = false)
	private BigDecimal totalAmount;// 銷貨總金額
	
	@Column(name = "order_date", nullable = false)
	private LocalDateTime orderDate;// 銷貨時間
	
	private String remark;// 銷貨備註
	
	// 💡 建立與明細檔的一對多關聯 (CascadeType.ALL 確保連動儲存明細)
	@OneToMany(mappedBy = "saleOrder", cascade = CascadeType.ALL ,orphanRemoval = true)
	private List<SaleOrderItem> items= new ArrayList<>();
	
	// 💡 方便新增明細的雙向關聯輔助方法
	public void addItem(SaleOrderItem item) {
		items.add(item);
		item.setSaleOrder(this);
	}
	
	@PrePersist
	public void onCreate() {
		if(this.orderDate == null) {
			this.orderDate = LocalDateTime.now();
		}
	}
}
