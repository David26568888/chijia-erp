package com.chijia.erp.service.impl;

import java.io.InputStream;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
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
public class SupplierServiceImpl implements SupplierService {

    @Autowired
    private SupplierRepository supplierRepository;

    @Autowired
    private SupplierMapper supplierMapper;

    @Override
    public List<SupplierDTO> getAllSuppliers() {
        return supplierRepository.findAll().stream()
                .map(supplierMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public SupplierDTO getSupplierById(Long id) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("找不到廠商，ID: " + id));
        return supplierMapper.toDTO(supplier);
    }

    @Override
    @Transactional
    public SupplierDTO createSupplier(SupplierDTO supplierDTO) {
        if (supplierDTO.getSupplierCode() != null && supplierRepository.existsBySupplierCode(supplierDTO.getSupplierCode())) {
            throw new RuntimeException("廠商編號已存在: " + supplierDTO.getSupplierCode());
        }
        Supplier supplier = supplierMapper.toEntity(supplierDTO);
        Supplier saved = supplierRepository.save(supplier);
        return supplierMapper.toDTO(saved);
    }

    @Override
    @Transactional
    public SupplierDTO updateSupplier(Long id, SupplierDTO supplierDTO) {
        Supplier existing = supplierRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("找不到廠商，ID: " + id));

        existing.setSupplierCode(supplierDTO.getSupplierCode());
        existing.setShortName(supplierDTO.getShortName());
        existing.setFullName(supplierDTO.getFullName());
        existing.setPhone(supplierDTO.getPhone());
        existing.setContactPerson(supplierDTO.getContactPerson());
        existing.setCompanyAddress(supplierDTO.getCompanyAddress());
        existing.setStatus(supplierDTO.isStatus());

        Supplier updated = supplierRepository.save(existing);
        return supplierMapper.toDTO(updated);
    }

    @Override
    @Transactional
    public void toggleStatus(Long id) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("找不到廠商，ID: " + id));
        supplier.setStatus(!supplier.isStatus());
        supplierRepository.save(supplier);
    }

    // 💡 1. 舊系統原始廠商報表匯入 (對應 廠商資料 Excel 結構，從第 4 行 i=3 開始)
    @Override
    @Transactional
    public String importSuppliersFromExcel(InputStream inputStream) throws Exception {
        int successCount = 0;
        int skipCount = 0;

        Set<String> existingCodes = supplierRepository.findAll().stream()
                .map(Supplier::getSupplierCode)
                .collect(Collectors.toSet());

        try (Workbook workbook = WorkbookFactory.create(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0); // 讀取 bsupp 工作表

            for (int i = 3; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                String code = ExcelHelper.getCellValueAsString(row.getCell(0)); // 0: 廠商編號
                String shortName = ExcelHelper.getCellValueAsString(row.getCell(1)); // 1: 廠商簡稱

                if (code.isEmpty() || shortName.isEmpty() || code.contains("*")) continue;

                if (existingCodes.contains(code)) {
                    skipCount++;
                    continue;
                }

                Supplier supplier = new Supplier();
                supplier.setSupplierCode(code);
                supplier.setShortName(shortName);
                supplier.setContactPerson(ExcelHelper.getCellValueAsString(row.getCell(3))); // 3: 聯絡人1
                supplier.setPhone(ExcelHelper.getCellValueAsString(row.getCell(4)));         // 4: 電話1
                supplier.setFullName(ExcelHelper.getCellValueAsString(row.getCell(14)));   // 14: 廠商名稱
                supplier.setCompanyAddress(ExcelHelper.getCellValueAsString(row.getCell(25)));       // 25: 公司地址
                supplier.setStatus(true);

                supplierRepository.save(supplier);
                existingCodes.add(code);
                successCount++;
            }
        }
        return String.format("原始廠商報表匯入完成！成功匯入 %d 筆，跳過重複 %d 筆。", successCount, skipCount);
    }

    // 💡 2. 還原「系統自身產生的廠商備份檔」(標準 6 欄格式，從第 1 行開始)
    @Override
    @Transactional
    public String restoreSuppliersFromBackup(InputStream inputStream) throws Exception {
        int successCount = 0;
        int skipCount = 0;

        Set<String> existingCodes = supplierRepository.findAll().stream()
                .map(Supplier::getSupplierCode)
                .collect(Collectors.toSet());

        try (Workbook workbook = WorkbookFactory.create(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                String code = ExcelHelper.getCellValueAsString(row.getCell(0));
                String shortName = ExcelHelper.getCellValueAsString(row.getCell(1));

                if (code.isEmpty() || shortName.isEmpty()) continue;

                if (existingCodes.contains(code)) {
                    skipCount++;
                    continue;
                }

                Supplier supplier = new Supplier();
                supplier.setSupplierCode(code);
                supplier.setShortName(shortName);
                supplier.setFullName(ExcelHelper.getCellValueAsString(row.getCell(2)));
                supplier.setPhone(ExcelHelper.getCellValueAsString(row.getCell(3)));
                supplier.setContactPerson(ExcelHelper.getCellValueAsString(row.getCell(4)));
                supplier.setCompanyAddress(ExcelHelper.getCellValueAsString(row.getCell(5)));
                supplier.setStatus(true);

                supplierRepository.save(supplier);
                existingCodes.add(code);
                successCount++;
            }
        }
        return String.format("廠商備份還原完成！成功匯入 %d 筆，跳過重複 %d 筆。", successCount, skipCount);
    }
}