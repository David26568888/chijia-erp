package com.chijia.erp.service.impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chijia.erp.model.dto.CreatePurchaseOrderDTO;
import com.chijia.erp.model.dto.PurchaseOrderDTO;
import com.chijia.erp.model.entity.Product;
import com.chijia.erp.model.entity.PurchaseOrder;
import com.chijia.erp.model.entity.PurchaseOrderItem;
import com.chijia.erp.repository.ProductRepository;
import com.chijia.erp.repository.PurchaseOrderRepository;
import com.chijia.erp.repository.SupplierRepository;
import com.chijia.erp.service.PurchaseOrderService;

@Service
public class PurchaseOrderServiceImpl implements PurchaseOrderService {

    @Autowired
    private PurchaseOrderRepository purchaseOrderRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private SupplierRepository supplierRepository;

    @Override
    @Transactional(rollbackFor = Exception.class) // 💡 資料庫事務管理，確保加庫存與寫單據同步成功或回滾
    public PurchaseOrderDTO createPurchaseOrder(CreatePurchaseOrderDTO createDTO) {

        // 1. 驗證廠商是否存在
        if (createDTO.getSupplierId() != null && !supplierRepository.existsById(createDTO.getSupplierId())) {
            throw new RuntimeException("找不到對應的廠商 ID: " + createDTO.getSupplierId());
        }

        // 2. 建立進貨單主檔
        PurchaseOrder purchaseOrder = new PurchaseOrder();
        purchaseOrder.setPurchaseNo(generatePurchaseNo()); // 自動生成單號 PO-YYYYMMDD-XXXX
        purchaseOrder.setSupplierId(createDTO.getSupplierId());
        purchaseOrder.setRemark(createDTO.getRemark());

        BigDecimal grandTotal = BigDecimal.ZERO;

        // 3. 處理每筆進貨明細 & 自動增加庫存 (補貨)
        if (createDTO.getItems() != null) {
            for (CreatePurchaseOrderDTO.CreateItemDTO itemDTO : createDTO.getItems()) {

                // 3a. 檢查進貨商品是否存在
                Product product = productRepository.findById(itemDTO.getProductId())
                        .orElseThrow(() -> new RuntimeException("商品不存在，ID: " + itemDTO.getProductId()));

                // 取出進貨數量 (若為 null 則帶預設 0.00)
                BigDecimal inQty = itemDTO.getQuantity() != null ? itemDTO.getQuantity() : BigDecimal.ZERO;

                // 3b. 💡 自動增加商品庫存！使用 .add() 進行 BigDecimal 運算 (現有庫存 + 進貨數量)
                BigDecimal currentStock = product.getStockQuantity() != null ? product.getStockQuantity() : BigDecimal.ZERO;
                product.setStockQuantity(currentStock.add(inQty));
                productRepository.save(product); // 更新商品庫存

                // 3c. 建立進貨明細 Entity
                PurchaseOrderItem orderItem = new PurchaseOrderItem();
                orderItem.setProductId(product.getId());
                orderItem.setProductName(product.getProductName());
                orderItem.setQuantity(inQty);

                // 3d. 決定進貨單價：若有傳入單價則以此為主，否則自動抓取商品預設成本進價 (costPrice)
                BigDecimal unitPrice = itemDTO.getUnitPrice();
                if (unitPrice == null) {
                    unitPrice = product.getCostPrice() != null ? product.getCostPrice() : BigDecimal.ZERO;
                }
                orderItem.setUnitPrice(unitPrice);

                // 3e. 💡 計算小計金額使用 .multiply()：單價 * 數量
                BigDecimal subtotal = unitPrice.multiply(inQty);
                orderItem.setSubtotal(subtotal);

                // 累加整張單據總金額
                grandTotal = grandTotal.add(subtotal);

                // 雙向關聯加入主檔
                purchaseOrder.addItem(orderItem);
            }
        }

        purchaseOrder.setTotalAmount(grandTotal);

        // 4. 存入資料庫 (連同明細一同寫入)
        PurchaseOrder savedOrder = purchaseOrderRepository.save(purchaseOrder);

        // 5. 轉為 PurchaseOrderDTO 回傳
        return convertToDTO(savedOrder);
    }

    @Override
    public PurchaseOrderDTO getPurchaseOrderById(Long id) {
        PurchaseOrder purchaseOrder = purchaseOrderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("找不到進貨單，ID: " + id));
        return convertToDTO(purchaseOrder);
    }

    // 💡 自動生成進貨單號邏輯 (格式：PO-YYYYMMDD-隨機4碼)
    private String generatePurchaseNo() {
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomStr = UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        return "PO-" + dateStr + "-" + randomStr;
    }

    // Entity 轉 DTO 轉換邏輯
    private PurchaseOrderDTO convertToDTO(PurchaseOrder entity) {
        PurchaseOrderDTO dto = new PurchaseOrderDTO();
        dto.setId(entity.getId());
        dto.setPurchaseNo(entity.getPurchaseNo());
        dto.setSupplierId(entity.getSupplierId());
        dto.setTotalAmount(entity.getTotalAmount());
        dto.setPurchaseDate(entity.getPurchaseDate());
        dto.setRemark(entity.getRemark());

        List<PurchaseOrderDTO.ItemDTO> itemDTOs = new ArrayList<>();
        if (entity.getItems() != null) {
            for (PurchaseOrderItem item : entity.getItems()) {
                PurchaseOrderDTO.ItemDTO itemDTO = new PurchaseOrderDTO.ItemDTO();
                itemDTO.setId(item.getId());
                itemDTO.setProductId(item.getProductId());
                itemDTO.setProductName(item.getProductName());
                itemDTO.setQuantity(item.getQuantity());
                itemDTO.setUnitPrice(item.getUnitPrice());
                itemDTO.setSubtotal(item.getSubtotal());
                itemDTOs.add(itemDTO);
            }
        }
        dto.setItems(itemDTOs);
        return dto;
    }
}