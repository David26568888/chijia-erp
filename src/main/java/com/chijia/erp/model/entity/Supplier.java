package com.chijia.erp.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Entity //這是一個資料庫實體類別
@Table(name = "supplier") //指定在 MySQL 中的資料表名稱為 supplier
public class Supplier {
	
	@Id// 標記主鍵
	@GeneratedValue(strategy = GenerationType.IDENTITY)//設定 MySQL 的 id 自增（AUTO_INCREMENT）
	private Long id;// (Long, PK, 自增) 改用大寫 Long，在 JPA 實務中更安全（預設為 null 而非 0）

	@Column(name = "supplier_code",nullable = false, unique = true,length = 50)
	// 對應資料庫底線欄位，設定不可為空、唯一值限制
	private String supplierCode;//廠商編號

	@Column(name = "short_name",length = 50)
	private String shortName; //廠商簡稱（例如：泳淼/佳晶）

	@Column(name ="full_name", length = 100)
	private String fullName; //廠商名稱

	@Column(name="contact_person", length = 50)
	private String contactPerson; //聯絡人1

	@Column(name = "phone" , length = 20)
	private String phone; //電話1

	@Column(name="mobile" , length=20)
	private String mobile; //行動電話1

	@Column(name="fax" , length = 20)
	private String fax; //傳真機

	@Column(name="tax_id" , length = 20)
	private String taxId; //統一編號

	@Column(name="company_address" , length = 255)
	private String companyAddress; //公司地址

	@Column(name="remark" , columnDefinition = "TEXT")
	private String remark; //備註

	@Column(name = "status" , nullable = false)
	private boolean status; //狀態（啟用/停用）

}