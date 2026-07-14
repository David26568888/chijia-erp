package com.chijia.erp.mapper;

import org.springframework.stereotype.Component;

import com.chijia.erp.model.dto.SupplierDTO;
import com.chijia.erp.model.entity.Supplier;

@Component
public class SupplierMapper {
	public SupplierDTO toDTO(Supplier entity) {
		if(entity == null) return null;
		
		SupplierDTO dto = new SupplierDTO();
        dto.setId(entity.getId());
        dto.setSupplierCode(entity.getSupplierCode());
        dto.setShortName(entity.getShortName());
        dto.setFullName(entity.getFullName());
        dto.setContactPerson(entity.getContactPerson());
        dto.setPhone(entity.getPhone());
        dto.setMobile(entity.getMobile());
        dto.setFax(entity.getFax());
        dto.setTaxId(entity.getTaxId());
        dto.setCompanyAddress(entity.getCompanyAddress());
        dto.setRemark(entity.getRemark());
        dto.setStatus(entity.isStatus());
        return dto;
	}
	
	public Supplier toEntity(SupplierDTO dto) {
		if(dto ==null) return null;
		
		Supplier entity = new Supplier();
		entity.setId(dto.getId());
        entity.setSupplierCode(dto.getSupplierCode());
        entity.setShortName(dto.getShortName());
        entity.setFullName(dto.getFullName());
        entity.setContactPerson(dto.getContactPerson());
        entity.setPhone(dto.getPhone());
        entity.setMobile(dto.getMobile());
        entity.setFax(dto.getFax());
        entity.setTaxId(dto.getTaxId());
        entity.setCompanyAddress(dto.getCompanyAddress());
        entity.setRemark(dto.getRemark());
        entity.setStatus(dto.isStatus());
        return entity;
	}
}
