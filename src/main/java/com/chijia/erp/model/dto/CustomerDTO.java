package com.chijia.erp.model.dto;

import lombok.Data;

@Data
public class CustomerDTO {
	private Long id; 
	
	private String customerCode;//客戶編號（例如：0002、1）
	private String shortName;//客戶簡稱（例如：林建華、林天來鴻達）
	private String fullName;//客戶名稱
	private String contactPerson;//聯絡人
	private String phone;//電話
	private String mobile;//行動電話
	private String taxId;//統一編號
    private String companyAddress; // 公司地址
    private Integer checkoutDay = 31; // 結帳日，預設31日
    private String invoiceType; // 發票類別 (例如: 三聯式)
    private String invoiceTitle; // 發票抬頭
    private String remark; // 備註
    private boolean status; // 狀態
	
}
