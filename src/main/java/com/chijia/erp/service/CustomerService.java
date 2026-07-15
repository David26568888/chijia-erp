package com.chijia.erp.service;

import java.io.InputStream;
import java.util.List;

import com.chijia.erp.model.dto.CustomerDTO;

public interface CustomerService {

	// 1. 查詢所有客戶 (給前端客戶管理列表使用)
	List<CustomerDTO> getAllCustomers();
	
	// 2. 透過 ID 查詢單一客戶 (修改資料時帶入原資料使用)
	CustomerDTO getCustomerById(Long id);
	
	// 3. 新增客戶 (包含重複客戶編號檢查)
	CustomerDTO creatCustomer(CustomerDTO customerDTO);
	
	// 4. 修改客戶資料
	CustomerDTO updateCustomer(Long id , CustomerDTO customerDTO);
	
	// 5. 切換啟用/停用狀態 (五金行實務上避免刪除客戶導致舊帳目崩潰，皆用狀態控制)
	void toggleStatus(Long id);
	
	//6. 💡 新增這行：解析並匯入客戶 Excel 資料
    String importCustomersFromExcel(InputStream inputStream) throws Exception;
}
