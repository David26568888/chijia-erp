package com.chijia.erp.mapper;

import org.springframework.stereotype.Component;

import com.chijia.erp.model.dto.ProductDTO;
import com.chijia.erp.model.entity.Product;

@Component
public class ProductMapper {

	// 將資料庫實體 (Entity) 轉換為前端需要的 (DTO)
	public ProductDTO toDTO(Product entity) {
		if(entity==null) return null;
		
		ProductDTO dto = new ProductDTO();
		dto.setId(entity.getId());
		dto.setProductCode(entity.getProductCode());
		dto.setProductName(entity.getProductName());
		dto.setBarcode(entity.getBarcode());
		dto.setUnit(entity.getUnit());
		dto.setSalePrice(entity.getSalePrice());
		dto.setStockQuantity(entity.getStockQuantity());
		dto.setStatus(entity.isStatus());
		
		return dto;
	}
	
	// 將前端傳過來的 (DTO) 轉換為資料庫儲存用的 (Entity)
	public Product toEntity(ProductDTO dto) {
		if(dto==null) return null;
		
		Product entity = new Product();
		entity.setId(dto.getId());
		entity.setProductCode(dto.getProductCode());
		entity.setProductName(dto.getProductName());
		entity.setBarcode(dto.getBarcode());
		entity.setUnit(dto.getUnit());
		entity.setSalePrice(dto.getSalePrice());
		entity.setStockQuantity(dto.getStockQuantity());
		entity.setStatus(dto.isStatus());
		
		// 注意：新增商品時，進價預設需在 Service 層另外透過專用權限表單處理，或在此設為預設值
		return entity;
	}
	
}
