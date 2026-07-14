package com.chijia.erp.model.dto;

import lombok.Data;

@Data
public class SupplierDTO {
	
	private Long id;//
	private String supplierCode;//廠商編號
	private String shortName; //廠商簡稱（例如：泳淼/佳晶）
	private String fullName; //廠商名稱
	private String contactPerson; //聯絡人1
	private String phone; //電話1
	private String mobile; //行動電話1
	private String fax; //傳真機
	private String taxId; //統一編號
	private String companyAddress; //公司地址
	private String remark; //備註
	private boolean status; //狀態（啟用/停用）

}
