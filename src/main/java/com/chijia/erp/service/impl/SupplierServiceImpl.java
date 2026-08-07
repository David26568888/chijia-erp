package com.chijia.erp.service.impl;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chijia.erp.mapper.SupplierMapper;
import com.chijia.erp.model.dto.SupplierDTO;
import com.chijia.erp.model.entity.Supplier;
import com.chijia.erp.repository.SupplierRepository;
import com.chijia.erp.service.SupplierService;
import com.chijia.erp.util.ExcelHelper;



@Service
public class SupplierServiceImpl implements SupplierService{
	
	@Autowired
	private SupplierMapper supplierMapper;
	
	@Autowired
	private SupplierRepository supplierRepository;

	@Override
	public List<SupplierDTO> getAllSuppliers() {
		// 撈出所有 Entity，透過 Stream 語法一個個轉成 DTO 回傳給前端
		return supplierRepository.findAll().stream()
				.map(supplierMapper::toDTO)
				.collect(Collectors.toList());
	}

	@Override
	public SupplierDTO getSupplierById(Long id) {
		// 若找不到該廠商，則拋出異常
		Supplier supplier = supplierRepository.findById(id)
				.orElseThrow( ()->new RuntimeException("找不到該廠商，ID:"+ id));
		return supplierMapper.toDTO(supplier);
	}

	@Override
	public SupplierDTO createSupplier(SupplierDTO supplierDTO) {
		// 商業邏輯檢查：廠商編號不能重複
		if(supplierRepository.findBySupplierCode(supplierDTO.getSupplierCode()).isPresent()) {
			throw new RuntimeException("廠商編號 [" + supplierDTO.getSupplierCode() + "] 已存在，無法新增!" );
		}
		// 轉為 Entity 並存入資料庫
		Supplier supplier = supplierMapper.toEntity(supplierDTO);
		supplier.setStatus(true);// 新增預設為啟用狀態
		Supplier saveSupplier = supplierRepository.save(supplier);
		
		return supplierMapper.toDTO(saveSupplier);
	}

	@Override
	@Transactional
	public SupplierDTO updateSupplier(Long id, SupplierDTO supplierDTO) {
		Supplier existingSupplier = supplierRepository.findById(id)
				.orElseThrow(()-> new RuntimeException("找不到該廠商，無法更新! ID:" + id));
		
		// 將前端傳入的新資料覆蓋過去 (排除掉自增的 ID 不動)
		existingSupplier.setShortName(supplierDTO.getShortName());
        existingSupplier.setFullName(supplierDTO.getFullName());
        existingSupplier.setContactPerson(supplierDTO.getContactPerson());
        existingSupplier.setPhone(supplierDTO.getPhone());
        existingSupplier.setMobile(supplierDTO.getMobile());
        existingSupplier.setFax(supplierDTO.getFax());
        existingSupplier.setTaxId(supplierDTO.getTaxId());
        existingSupplier.setCompanyAddress(supplierDTO.getCompanyAddress());
        existingSupplier.setRemark(supplierDTO.getRemark());
        
        Supplier updatSupplier = supplierRepository.save(existingSupplier);
        return supplierMapper.toDTO(updatSupplier);
	}

	@Override
	@Transactional
	public void toggleStatus(Long id) {
		Supplier supplier = supplierRepository.findById(id)
				.orElseThrow(()-> new RuntimeException("找不到該廠商，無法切換狀態! ID:" + id));
		
		// 狀態反轉 (!true = false, !false = true)
		supplier.setStatus(!supplier.isStatus());
		supplierRepository.save(supplier);
	}

	@Override
	@Transactional(rollbackFor = Exception.class) // 💡 若出錯整批 Rollback，不污染資料庫
	public String importSuppliersFromExcel(InputStream inputStream) throws Exception {
	    Workbook workbook = WorkbookFactory.create(inputStream);
	    
	    // 獲取廠商資料專屬 Sheet "bsupp"
	    Sheet sheet = workbook.getSheet("bsupp");
	    if (sheet == null) {
	        sheet = workbook.getSheetAt(0);
	    }
	    
	    List<Supplier> supplierList = new ArrayList<>();
	    int importCount = 0;
	    
	    // 💡 修正 1：前 3 列為抬頭資訊，第 4 列 (Index=3) 為標題，真正的資料從第 5 列 (Index=4) 開始！
	    for (int i = 4; i <= sheet.getLastRowNum(); i++) {
	        Row row = sheet.getRow(i);
	        if (row == null) {
	            continue;
	        }
	        
	        String supplierCode = ExcelHelper.getCellValueAsString(row.getCell(0));
	        String shortName    = ExcelHelper.getCellValueAsString(row.getCell(1));
	        
	        // 💡 修正 2：如果編號與簡稱皆空白，或是星號跳過
	        if ((supplierCode.isEmpty() && shortName.isEmpty()) || "*".equals(supplierCode)) {
	            continue;
	        }
	        
	        // 若廠商編號為空但有簡稱，自動補預設編號，防止 DB 鍵值異常
	        if (supplierCode.isEmpty()) {
	            supplierCode = "SUP_" + i;
	        }
	        
	        Supplier supplier = new Supplier();
	        supplier.setSupplierCode(supplierCode);                                   // Col 0: 廠商編號
	        supplier.setShortName(shortName);                                        // Col 1: 廠商簡稱
	        supplier.setPhone(ExcelHelper.getCellValueAsString(row.getCell(4)));     // Col 4: 電話1
	        supplier.setFullName(ExcelHelper.getCellValueAsString(row.getCell(14))); // Col 14: 廠商名稱
	        supplier.setTaxId(ExcelHelper.getCellValueAsString(row.getCell(21)));    // Col 21: 統一編號
	        supplier.setCompanyAddress(ExcelHelper.getCellValueAsString(row.getCell(25))); // Col 25: 公司地址
	        supplier.setStatus(true);
	        
	        // 防重複匯入：存在則更新，不存在則新增
	        Optional<Supplier> existingSupplierOpt = supplierRepository.findBySupplierCode(supplierCode);
	        if (existingSupplierOpt.isPresent()) {
	            Supplier existingSupplier = existingSupplierOpt.get();
	            existingSupplier.setShortName(supplier.getShortName());
	            existingSupplier.setPhone(supplier.getPhone());
	            existingSupplier.setFullName(supplier.getFullName());
	            existingSupplier.setTaxId(supplier.getTaxId());
	            existingSupplier.setCompanyAddress(supplier.getCompanyAddress());
	            supplierList.add(existingSupplier);
	        } else {
	            supplierList.add(supplier);
	        }
	        
	        importCount++;  
	    }
	    
	    if (!supplierList.isEmpty()) {
	        supplierRepository.saveAll(supplierList);
	    }
	        
	    workbook.close();
	    return "成功匯入/更新 " + importCount + " 筆廠商資料！";
	}

}
