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
	@Transactional(rollbackFor = Exception.class)// 💡 確保若匯入出錯，整批 Rollback，不污染資料庫
	public String importSuppliersFromExcel(InputStream inputStream) throws Exception{
		// 1. 使用 POI 的 WorkbookFactory 自動辨識是 .xls 還是 .xlsx
		Workbook workbook = WorkbookFactory.create(inputStream);
		
		// 2. 獲取廠商資料專屬的 Sheet 名稱 "bsupp"
		Sheet sheet = workbook.getSheet("bsupp");
		if(sheet == null) {
			// 防呆：如果找不到該分頁，嘗試拿第一個 Sheet
			sheet = workbook.getSheetAt(0);
		}
		
		List<Supplier> supplierList = new ArrayList<>();
		int importCount = 0;
		
		// 3. 遍歷每一列 (老牌系統前三行是標題，第四行 Index = 3 開始是數據)
		for(int i= 3 ;i <= sheet.getLastRowNum() ;i ++) {
			Row row = sheet.getRow(i);
			if(row ==null) {
				continue;
			}
			// 讀取第 0 欄：廠商編號，若為空代表這行是空行，結束或跳過
			String supplierCode = ExcelHelper.getCellValueAsString(row.getCell(0));
			if(supplierCode.isEmpty() || "*".equals(supplierCode)) {
				continue;
			}
			
			// 4. 對齊 Excel 欄位並封裝成 Supplier Entity 物件
			Supplier supplier = new Supplier();
			
			supplier.setSupplierCode(supplierCode);
			supplier.setShortName(ExcelHelper.getCellValueAsString(row.getCell(1)));
			supplier.setPhone(ExcelHelper.getCellValueAsString(row.getCell(4)));
			supplier.setFullName(ExcelHelper.getCellValueAsString(row.getCell(14)));
			supplier.setTaxId(ExcelHelper.getCellValueAsString(row.getCell(21)));
			supplier.setCompanyAddress(ExcelHelper.getCellValueAsString(row.getCell(25)));
			supplier.setStatus(true);
			
			// 5. 實務設計：防重複匯入（利用廠商編號判斷）
            // 如果資料庫已經有這個廠商，就更新它；沒有則新增。
			Optional<Supplier> existingSupplierOpt = supplierRepository.findBySupplierCode(supplierCode);
			if(existingSupplierOpt.isPresent()) {
				Supplier existingSupplier = existingSupplierOpt.get();
				existingSupplier.setShortName(supplier.getShortName());
				existingSupplier.setPhone(supplier.getPhone());
				existingSupplier.setFullName(supplier.getFullName());
				existingSupplier.setTaxId(supplier.getTaxId());
				existingSupplier.setCompanyAddress(supplier.getCompanyAddress());
				supplierList.add(existingSupplier);
			}else {
				// 💡 如果不存在，直接新增
				supplierList.add(supplier);
			}
			
			importCount++;	
		}
		
		// 6. 批次儲存到資料庫
        if (!supplierList.isEmpty()) {
            supplierRepository.saveAll(supplierList);
        }
			
        workbook.close();
		return "成功匯入/更新 " + importCount + " 筆廠商資料！";
	}

}
