package com.chijia.erp.service;

import java.io.InputStream;
import java.util.List;

import com.chijia.erp.model.dto.SupplierDTO;

public interface SupplierService {
	
	// 1. 查詢所有廠商 (給廠商列表清單使用)
    List<SupplierDTO> getAllSuppliers();
    
    // 2. 透過 ID 查詢單一廠商 (修改資料時帶入原資料使用)
    SupplierDTO getSupplierById(Long id);
    
    // 3. 新增廠商 (包含重複編號檢查)
    SupplierDTO createSupplier(SupplierDTO supplierDTO);
    
    // 4. 修改廠商資料
    SupplierDTO updateSupplier(Long id, SupplierDTO supplierDTO);
    
    // 5. 切換啟用/停用狀態 (五金行實務上很少直接刪除廠商，多用狀態控制以免舊帳目壞掉)
    void toggleStatus(Long id);
    
    //6. 解析並大量匯入廠商 Excel 資料
    /**
    * @param inputStream 上傳檔案的輸入流
    * @return 匯入結果訊息
    */
    String importSuppliersFromExcel(InputStream intputStream) throws Exception;

}
