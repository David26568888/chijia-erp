package com.chijia.erp.service.impl;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.chijia.erp.mapper.SupplierMapper;
import com.chijia.erp.model.dto.SupplierDTO;
import com.chijia.erp.model.entity.Supplier;
import com.chijia.erp.repository.SupplierRepository;
import com.chijia.erp.service.SupplierService;

import jakarta.transaction.Transactional;

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
	

}
