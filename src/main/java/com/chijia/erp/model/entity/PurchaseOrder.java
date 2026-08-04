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
@Table(name = "purchase_order")
public class PurchaseOrder {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	@Column(name="purchase_no" ,nullable = false,unique = true,length = 30)
	private String purchaseNo;// 進貨單號 (例如: PO-20260802-A1B2)
	
	@Column(name = "supplier_id",nullable = false)
	private Long supplierId;// 廠商ID
	
	@Column(name ="totalAmount",nullable = false)
	private BigDecimal totalAmount;// 進貨總金額
	
	@Column(name = "purchase_date" ,nullable=false)
	private LocalDateTime purchaseDate;// 進貨時間
	
	@Column(length = 200)
	private String remark;// 進貨備註
	
	// 💡 建立與進貨明細檔的一對多關聯 (CascadeType.ALL 確保新增進貨單時連同明細自動寫入)
	@OneToMany(mappedBy = "purchaseOrder",cascade = CascadeType.ALL, orphanRemoval = true)
	private List<PurchaseOrderItem> items = new ArrayList<>();
	
	public void addItem(PurchaseOrderItem item){
		items.add(item);
		item.setPurchaseOrder(this);
	}
	
	@PrePersist
	/*
	 * purchaseDate 欄位設有 nullable = false，
	 * 資料庫會直接拋出 PropertyValueException: 
	 * not-null property references a null 
	 * or transient value 錯誤而崩潰！
	 * */
	public void onCreate() {
		if(this.purchaseDate == null) {
			this.purchaseDate = LocalDateTime.now();
		}
	}
}
