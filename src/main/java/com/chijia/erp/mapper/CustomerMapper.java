package com.chijia.erp.mapper;

import org.springframework.stereotype.Component;

import com.chijia.erp.model.dto.CustomerDTO;
import com.chijia.erp.model.entity.Customer;

@Component
public class CustomerMapper {

    // 將資料庫實體 (Entity) 轉換為前端需要的 (DTO)
    public CustomerDTO toDTO(Customer entity) {
        if (entity == null) return null;

        CustomerDTO dto = new CustomerDTO();
        dto.setId(entity.getId());
        dto.setCustomerCode(entity.getCustomerCode());
        dto.setShortName(entity.getShortName());
        dto.setFullName(entity.getFullName());
        dto.setContactPerson(entity.getContactPerson());
        dto.setPhone(entity.getPhone());
        dto.setMobile(entity.getMobile());
        dto.setFax(entity.getFax()); // 💡 補齊傳真
        dto.setTaxId(entity.getTaxId());
        dto.setEmail(entity.getEmail()); // 💡 補齊 Email
        dto.setAddress(entity.getAddress()); // 💡 修正對齊 address 屬性
        dto.setDeliveryAddress(entity.getDeliveryAddress()); // 💡 補齊送貨地址
        dto.setCheckoutDay(entity.getCheckoutDay());
        dto.setInvoiceType(entity.getInvoiceType());
        dto.setInvoiceTitle(entity.getInvoiceTitle());
        dto.setRemark(entity.getRemark());
        dto.setStatus(entity.isStatus());

        return dto;
    }

    // 將前端傳過來的 (DTO) 轉換為資料庫儲存用的 (Entity)
    public Customer toEntity(CustomerDTO dto) {
        if (dto == null) return null;

        Customer entity = new Customer();
        entity.setId(dto.getId());
        entity.setCustomerCode(dto.getCustomerCode());
        entity.setShortName(dto.getShortName());
        entity.setFullName(dto.getFullName());
        entity.setContactPerson(dto.getContactPerson());
        entity.setPhone(dto.getPhone());
        entity.setMobile(dto.getMobile());
        entity.setFax(dto.getFax()); // 💡 補齊傳真
        entity.setTaxId(dto.getTaxId());
        entity.setEmail(dto.getEmail()); // 💡 補齊 Email
        entity.setAddress(dto.getAddress()); // 💡 修正對齊 address 屬性
        entity.setDeliveryAddress(dto.getDeliveryAddress()); // 💡 補齊送貨地址
        entity.setCheckoutDay(dto.getCheckoutDay() != null ? dto.getCheckoutDay() : 31); // 💡 修正 Bug：帶入 dto 結帳日
        entity.setInvoiceType(dto.getInvoiceType());
        entity.setInvoiceTitle(dto.getInvoiceTitle());
        entity.setRemark(dto.getRemark());
        entity.setStatus(dto.isStatus());

        return entity;
    }
}