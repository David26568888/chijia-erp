package com.chijia.erp.service;

import java.io.InputStream;
import java.util.List;

import com.chijia.erp.model.dto.CustomerDTO;

public interface CustomerService {

    // 1. 查詢所有客戶 (給前端客戶管理列表使用)
    List<CustomerDTO> getAllCustomers();

    // 2. 依據關鍵字模糊搜尋客戶 (支援名稱、代碼、電話快搜)
    List<CustomerDTO> searchCustomers(String keyword);

    // 3. 透過 ID 查詢單一客戶 (修改資料時帶入原資料使用)
    CustomerDTO getCustomerById(Long id);

    // 4. 新增客戶 (包含重複客戶編號檢查)[cite: 17]
    CustomerDTO createCustomer(CustomerDTO customerDTO);

    // 5. 修改客戶資料[cite: 17]
    CustomerDTO updateCustomer(Long id, CustomerDTO customerDTO);

    // 6. 切換啟用/停用狀態 (五金行實務上避免刪除客戶導致舊帳目崩潰，皆用狀態控制)[cite: 17]
    void toggleStatus(Long id);

    // 7. 💡 解析舊ERP並匯入客戶 Excel 資料 (災難復原與備份還原專用)
    String importCustomersFromExcel(InputStream inputStream) throws Exception;

 // 8. 💡 解析新系統excel 並匯入客戶 Excel 資料 (災難復原與備份還原專用)
	String restoreCustomersFromBackup(InputStream inputStream) throws Exception;
}