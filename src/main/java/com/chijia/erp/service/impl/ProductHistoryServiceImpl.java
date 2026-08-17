package com.chijia.erp.service.impl;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chijia.erp.model.dto.ProductHistoryDTO;
import com.chijia.erp.model.entity.PurchaseOrderItem;
import com.chijia.erp.model.entity.SaleOrderItem;
import com.chijia.erp.repository.PurchaseOrderItemRepository;
import com.chijia.erp.repository.SaleOrderItemRepository;
import com.chijia.erp.service.ProductHistoryService;

@Service
@Transactional(readOnly = true)
public class ProductHistoryServiceImpl implements ProductHistoryService {

    @Autowired
    private PurchaseOrderItemRepository purchaseOrderItemRepository;

    @Autowired
    private SaleOrderItemRepository saleOrderItemRepository;

    @Override
    public ProductHistoryDTO getProductHistory(Long productId) {
        // 1. 查詢近 10 筆銷售紀錄
        List<SaleOrderItem> salesItems = saleOrderItemRepository.findTop10ByProductIdOrderBySaleOrderSaleDateDesc(productId);
        List<ProductHistoryDTO.SaleRecordDTO> salesRecords = salesItems.stream().map(item -> {
            ProductHistoryDTO.SaleRecordDTO record = new ProductHistoryDTO.SaleRecordDTO();
            record.setSaleDate(item.getSaleOrder() != null ? item.getSaleOrder().getSaleDate() : null);
            record.setCustomerName(
                (item.getSaleOrder() != null && item.getSaleOrder().getCustomer() != null) 
                    ? item.getSaleOrder().getCustomer().getShortName() 
                    : "散客"
            );
            record.setUnitPrice(item.getUnitPrice());
            record.setQuantity(item.getQuantity());
            return record;
        }).collect(Collectors.toList());

        // 2. 查詢進貨歷史
        List<PurchaseOrderItem> purchaseItems = purchaseOrderItemRepository.findTop10ByProductIdOrderByPurchaseOrderPurchaseDateDesc(productId);
        List<ProductHistoryDTO.PurchaseRecordDTO> purchaseRecords = purchaseItems.stream().map(item -> {
            ProductHistoryDTO.PurchaseRecordDTO record = new ProductHistoryDTO.PurchaseRecordDTO();
            record.setPurchaseDate(item.getPurchaseOrder() != null ? item.getPurchaseOrder().getPurchaseDate() : null);
            record.setSupplierName(
                (item.getPurchaseOrder() != null && item.getPurchaseOrder().getSupplier() != null) 
                    ? item.getPurchaseOrder().getSupplier().getShortName() 
                    : "未知廠商"
            );
            record.setUnitPrice(item.getUnitPrice());
            record.setQuantity(item.getQuantity());
            return record;
        }).collect(Collectors.toList());

        // 💡 3. 先實例化 DTO 物件，再打包回傳
        ProductHistoryDTO dto = new ProductHistoryDTO();
        dto.setSaleHistory(salesRecords);
        dto.setPurchaseHistory(purchaseRecords);
        return dto;
    }
}