package com.chijia.erp.mapper;

import org.springframework.stereotype.Component;

import com.chijia.erp.model.dto.ProductDTO;
import com.chijia.erp.model.entity.Product;

@Component
public class ProductMapper {

    /**
     * Entity 轉 DTO
     */
    public ProductDTO toDTO(Product entity) {
        if (entity == null) {
            return null;
        }

        ProductDTO dto = new ProductDTO();
        dto.setId(entity.getId());
        dto.setProductCode(entity.getProductCode());
        dto.setProductName(entity.getProductName());
        dto.setBarcode(entity.getBarcode());
        dto.setUnit(entity.getUnit());
        dto.setSalePrice(entity.getSalePrice());

        // 💡 三軌成本手動對應
        dto.setCostPrice(entity.getCostPrice());         // 1. 預設 / 基準成本
        dto.setLastCostPrice(entity.getLastCostPrice()); // 2. 最後進價
        dto.setAvgCostPrice(entity.getAvgCostPrice());   // 3. 移動加權平均成本

        dto.setStockQuantity(entity.getStockQuantity());
        dto.setSafetyStock(entity.getSafetyStock());
        dto.setStatus(entity.isStatus());

        return dto;
    }

    /**
     * DTO 轉 Entity
     */
    public Product toEntity(ProductDTO dto) {
        if (dto == null) {
            return null;
        }

        Product product = new Product();
        product.setId(dto.getId());
        product.setProductCode(dto.getProductCode());
        product.setProductName(dto.getProductName());
        product.setBarcode(dto.getBarcode());
        product.setUnit(dto.getUnit());
        product.setSalePrice(dto.getSalePrice());

        // 💡 三軌成本手動對應
        product.setCostPrice(dto.getCostPrice());
        product.setLastCostPrice(dto.getLastCostPrice());
        product.setAvgCostPrice(dto.getAvgCostPrice());

        if (dto.getStockQuantity() != null) {
            product.setStockQuantity(dto.getStockQuantity());
        }
        if (dto.getSafetyStock() != null) {
            product.setSafetyStock(dto.getSafetyStock());
        }
        product.setStatus(dto.isStatus());

        return product;
    }
}