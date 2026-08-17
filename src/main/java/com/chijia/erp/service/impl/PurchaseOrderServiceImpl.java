package com.chijia.erp.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chijia.erp.model.dto.CreatePurchaseOrderDTO;
import com.chijia.erp.model.dto.PurchaseOrderDTO;
import com.chijia.erp.model.entity.Product;
import com.chijia.erp.model.entity.PurchaseOrder;
import com.chijia.erp.model.entity.PurchaseOrderItem;
import com.chijia.erp.model.entity.Supplier;
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

    // 1. 查詢所有進貨單 (依 ID 倒序)
    @Override
    public List<PurchaseOrderDTO> getAllPurchaseOrders() {
        return purchaseOrderRepository.findAll().stream()
                .sorted((a, b) -> b.getId().compareTo(a.getId()))
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // 2. 多條件搜尋進貨單
    @Override
    public List<PurchaseOrderDTO> searchPurchaseOrders(String keyword, LocalDate startDate, LocalDate endDate) {
        List<PurchaseOrder> orders = purchaseOrderRepository.findAll();

        return orders.stream()
                .filter(o -> {
                    boolean matchKw = true;
                    if (keyword != null && !keyword.trim().isEmpty()) {
                        String kw = keyword.trim().toLowerCase();
                        boolean matchNo = o.getPurchaseNo() != null && o.getPurchaseNo().toLowerCase().contains(kw);
                        
                        String suppName = (o.getSupplier() != null && o.getSupplier().getShortName() != null)
                                ? o.getSupplier().getShortName() : "";
                        boolean matchSupp = suppName.toLowerCase().contains(kw);
                        matchKw = matchNo || matchSupp;
                    }

                    boolean matchDate = true;
                    if (startDate != null && o.getPurchaseDate() != null) {
                        matchDate = !o.getPurchaseDate().isBefore(startDate);
                    }
                    if (endDate != null && o.getPurchaseDate() != null && matchDate) {
                        matchDate = !o.getPurchaseDate().isAfter(endDate);
                    }

                    return matchKw && matchDate;
                })
                .sorted((a, b) -> b.getId().compareTo(a.getId()))
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // 3. 依 ID 查詢進貨單
    @Override
    public PurchaseOrderDTO getPurchaseOrderById(Long id) {
        PurchaseOrder purchaseOrder = purchaseOrderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("找不到進貨單，ID: " + id));
        return convertToDTO(purchaseOrder);
    }

    // 4. 新增進貨單 (裝配 Supplier & Product 物件 + 增加庫存 + 更新三軌成本)
    @Override
    @Transactional(rollbackFor = Exception.class)
    public PurchaseOrderDTO createPurchaseOrder(CreatePurchaseOrderDTO createDTO) {
        Supplier supplier = supplierRepository.findById(createDTO.getSupplierId())
                .orElseThrow(() -> new RuntimeException("找不到對應的廠商 ID: " + createDTO.getSupplierId()));

        PurchaseOrder purchaseOrder = new PurchaseOrder();
        purchaseOrder.setPurchaseNo(generatePurchaseNo());
        purchaseOrder.setSupplier(supplier); // 💡 正確設置 Supplier 物件關聯
        purchaseOrder.setPurchaseDate(createDTO.getPurchaseDate() != null ? createDTO.getPurchaseDate() : LocalDate.now());
        purchaseOrder.setRemark(createDTO.getRemark());
        purchaseOrder.setDiscountAmount(createDTO.getDiscountAmount() != null ? createDTO.getDiscountAmount() : BigDecimal.ZERO);

        BigDecimal grandTotal = BigDecimal.ZERO;

        if (createDTO.getItems() != null) {
            for (CreatePurchaseOrderDTO.CreateItemDTO itemDTO : createDTO.getItems()) {
                Product product = productRepository.findById(itemDTO.getProductId())
                        .orElseThrow(() -> new RuntimeException("商品不存在，ID: " + itemDTO.getProductId()));

                BigDecimal inQty = itemDTO.getQuantity() != null ? itemDTO.getQuantity() : BigDecimal.ZERO;
                BigDecimal currentStock = product.getStockQuantity() != null ? product.getStockQuantity() : BigDecimal.ZERO;

                // 決定進貨單價
                BigDecimal unitPrice = itemDTO.getUnitPrice();
                if (unitPrice == null) {
                    unitPrice = product.getCostPrice() != null ? product.getCostPrice() : BigDecimal.ZERO;
                }

                // 💡 三軌成本自動計算與更新：
                // 1) 更新最後進價 (lastCostPrice)
                product.setLastCostPrice(unitPrice);

                // 2) 計算加權移動平均成本
                BigDecimal oldAvgCost = product.getAvgCostPrice() != null ? product.getAvgCostPrice() : (product.getCostPrice() != null ? product.getCostPrice() : BigDecimal.ZERO);
                BigDecimal oldTotalVal = currentStock.multiply(oldAvgCost);
                BigDecimal newInVal = inQty.multiply(unitPrice);
                BigDecimal newTotalQty = currentStock.add(inQty);

                if (newTotalQty.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal newAvgCost = oldTotalVal.add(newInVal).divide(newTotalQty, 2, RoundingMode.HALF_UP);
                    product.setAvgCostPrice(newAvgCost);
                }

                // 3) 自動增加庫存
                product.setStockQuantity(newTotalQty);
                productRepository.save(product);

                // 建立明細項
                PurchaseOrderItem orderItem = new PurchaseOrderItem();
                orderItem.setProduct(product); // 💡 正確設置 Product 物件關聯
                orderItem.setProductName(product.getProductName());
                orderItem.setProductCode(product.getProductCode());
                orderItem.setQuantity(inQty);
                orderItem.setUnitPrice(unitPrice);

                BigDecimal subtotal = unitPrice.multiply(inQty);
                orderItem.setSubtotal(subtotal);

                grandTotal = grandTotal.add(subtotal);
                purchaseOrder.addItem(orderItem);
            }
        }

        BigDecimal finalTotal = grandTotal.subtract(purchaseOrder.getDiscountAmount());
        purchaseOrder.setTotalAmount(finalTotal.compareTo(BigDecimal.ZERO) > 0 ? finalTotal : BigDecimal.ZERO);

        PurchaseOrder savedOrder = purchaseOrderRepository.save(purchaseOrder);
        return convertToDTO(savedOrder);
    }

    // 5. 修改進貨單 (校正庫存與關聯)
    @Override
    @Transactional(rollbackFor = Exception.class)
    public PurchaseOrderDTO updatePurchaseOrder(Long id, CreatePurchaseOrderDTO updateDTO) {
        PurchaseOrder existingOrder = purchaseOrderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("找不到該進貨單，無法更新！ID: " + id));

        // 步驟 A：將舊進貨單增加的庫存扣回
        if (existingOrder.getItems() != null) {
            for (PurchaseOrderItem oldItem : existingOrder.getItems()) {
                if (oldItem.getProduct() != null) {
                    Product product = oldItem.getProduct();
                    BigDecimal oldQty = oldItem.getQuantity() != null ? oldItem.getQuantity() : BigDecimal.ZERO;
                    BigDecimal currentStock = product.getStockQuantity() != null ? product.getStockQuantity() : BigDecimal.ZERO;
                    product.setStockQuantity(currentStock.subtract(oldQty));
                    productRepository.save(product);
                }
            }
            existingOrder.getItems().clear();
        }

        // 步驟 B：更新 Supplier 與主檔資訊
        Supplier supplier = supplierRepository.findById(updateDTO.getSupplierId())
                .orElseThrow(() -> new RuntimeException("找不到對應的廠商 ID: " + updateDTO.getSupplierId()));
        existingOrder.setSupplier(supplier);

        if (updateDTO.getPurchaseDate() != null) {
            existingOrder.setPurchaseDate(updateDTO.getPurchaseDate());
        }
        existingOrder.setRemark(updateDTO.getRemark());
        existingOrder.setDiscountAmount(updateDTO.getDiscountAmount() != null ? updateDTO.getDiscountAmount() : BigDecimal.ZERO);

        // 步驟 C：重新計算新明細並增加庫存
        BigDecimal grandTotal = BigDecimal.ZERO;

        if (updateDTO.getItems() != null) {
            for (CreatePurchaseOrderDTO.CreateItemDTO itemDTO : updateDTO.getItems()) {
                Product product = productRepository.findById(itemDTO.getProductId())
                        .orElseThrow(() -> new RuntimeException("商品不存在，ID: " + itemDTO.getProductId()));

                BigDecimal inQty = itemDTO.getQuantity() != null ? itemDTO.getQuantity() : BigDecimal.ZERO;
                BigDecimal currentStock = product.getStockQuantity() != null ? product.getStockQuantity() : BigDecimal.ZERO;
                BigDecimal unitPrice = itemDTO.getUnitPrice() != null ? itemDTO.getUnitPrice() : (product.getCostPrice() != null ? product.getCostPrice() : BigDecimal.ZERO);

                product.setLastCostPrice(unitPrice);
                product.setStockQuantity(currentStock.add(inQty));
                productRepository.save(product);

                PurchaseOrderItem orderItem = new PurchaseOrderItem();
                orderItem.setProduct(product);
                orderItem.setProductName(product.getProductName());
                orderItem.setProductCode(product.getProductCode());
                orderItem.setQuantity(inQty);
                orderItem.setUnitPrice(unitPrice);

                BigDecimal subtotal = unitPrice.multiply(inQty);
                orderItem.setSubtotal(subtotal);

                grandTotal = grandTotal.add(subtotal);
                existingOrder.addItem(orderItem);
            }
        }

        BigDecimal finalTotal = grandTotal.subtract(existingOrder.getDiscountAmount());
        existingOrder.setTotalAmount(finalTotal.compareTo(BigDecimal.ZERO) > 0 ? finalTotal : BigDecimal.ZERO);

        PurchaseOrder updatedOrder = purchaseOrderRepository.save(existingOrder);
        return convertToDTO(updatedOrder);
    }

    // 6. 作廢/刪除進貨單 (自動將庫存扣回)
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePurchaseOrder(Long id) {
        PurchaseOrder purchaseOrder = purchaseOrderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("找不到進貨單，無法作廢！ID: " + id));

        if (purchaseOrder.getItems() != null) {
            for (PurchaseOrderItem item : purchaseOrder.getItems()) {
                if (item.getProduct() != null) {
                    Product product = item.getProduct();
                    BigDecimal currentStock = product.getStockQuantity() != null ? product.getStockQuantity() : BigDecimal.ZERO;
                    BigDecimal itemQty = item.getQuantity() != null ? item.getQuantity() : BigDecimal.ZERO;
                    product.setStockQuantity(currentStock.subtract(itemQty));
                    productRepository.save(product);
                }
            }
        }

        purchaseOrderRepository.delete(purchaseOrder);
    }

    private String generatePurchaseNo() {
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomStr = UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        return "PO-" + dateStr + "-" + randomStr;
    }

    // DTO 轉化封裝
    private PurchaseOrderDTO convertToDTO(PurchaseOrder entity) {
        PurchaseOrderDTO dto = new PurchaseOrderDTO();
        dto.setId(entity.getId());
        dto.setPurchaseNo(entity.getPurchaseNo());
        
        if (entity.getSupplier() != null) {
            dto.setSupplierId(entity.getSupplier().getId());
            dto.setSupplierName(entity.getSupplier().getShortName() != null ? entity.getSupplier().getShortName() : entity.getSupplier().getFullName());
        }
        
        dto.setPurchaseDate(entity.getPurchaseDate());
        dto.setRemark(entity.getRemark());
        dto.setDiscountAmount(entity.getDiscountAmount());
        dto.setTotalAmount(entity.getTotalAmount());

        List<PurchaseOrderDTO.ItemDTO> itemDTOs = new ArrayList<>();
        if (entity.getItems() != null) {
            for (PurchaseOrderItem item : entity.getItems()) {
                PurchaseOrderDTO.ItemDTO itemDTO = new PurchaseOrderDTO.ItemDTO();
                itemDTO.setId(item.getId());
                if (item.getProduct() != null) {
                    itemDTO.setProductId(item.getProduct().getId());
                }
                itemDTO.setProductName(item.getProductName());
                itemDTO.setProductCode(item.getProductCode());
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