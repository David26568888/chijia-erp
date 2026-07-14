package com.chijia.erp.mapper;

import org.springframework.stereotype.Component;

import com.chijia.erp.model.dto.CustomerDTO;
import com.chijia.erp.model.entity.Customer;

@Component
public class CustomerMapper {
	
	// 將資料庫實體 (Entity) 轉換為前端需要的 (DTO)
	public CustomerDTO toDTO(Customer entity) {
		if(entity == null) return null;
		
		CustomerDTO dto = new CustomerDTO();
		dto.setId(entity.getId());
		dto.setCustomerCode(entity.getCustomerCode());
		dto.setShortName(entity.getShortName());
		dto.setFullName(entity.getFullName());
		dto.setContactPerson(entity.getContactPerson());
		dto.setPhone(entity.getPhone());
		dto.setMobile(entity.getMobile());
		dto.setTaxId(entity.getTaxId());
		dto.setCompanyAddress(entity.getCompanyAddress());
		dto.setCheckoutDay(entity.getCheckoutDay());
		dto.setInvoiceType(entity.getInvoiceType());
		dto.setInvoiceTitle(entity.getInvoiceTitle());
		dto.setRemark(entity.getRemark());
		dto.setStatus(entity.isStatus());
		
		return dto;
	}
	
	// 將前端傳過來的 (DTO) 轉換為資料庫儲存用的 (Entity)
	public  Customer toEntity(CustomerDTO dto) {
		if(dto == null) return null;
		
		Customer entity = new Customer();
		entity.setId(dto.getId());
		entity.setCustomerCode(dto.getCustomerCode());
		entity.setShortName(dto.getShortName());
		entity.setFullName(dto.getFullName());
		entity.setContactPerson(dto.getContactPerson());
		entity.setPhone(dto.getPhone());
		entity.setMobile(dto.getMobile());
		entity.setTaxId(dto.getTaxId());
		entity.setCompanyAddress(dto.getCompanyAddress());
		entity.setCheckoutDay(entity.getCheckoutDay());
		entity.setInvoiceType(dto.getInvoiceType());
		entity.setInvoiceTitle(dto.getInvoiceTitle());
		entity.setRemark(dto.getRemark());
		entity.setStatus(dto.isStatus());
		
		return entity;
	}
}
