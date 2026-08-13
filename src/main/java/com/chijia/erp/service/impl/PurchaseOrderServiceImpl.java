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

    // 1. 查詢所有進貨單 (依 ID 倒序，最新開單的排最前面)
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
                        
                        String suppName = "";
                        if (o.getSupplierId() != null) {
                            suppName = supplierRepository.findById(o.getSupplierId())
                                    .map(Supplier::getShortName).orElse("");
                        }
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

    // 3. 依 ID 查詢進貨單[cite: 16]
    @Override
    public PurchaseOrderDTO getPurchaseOrderById(Long id) {
        PurchaseOrder purchaseOrder = purchaseOrderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("找不到進貨單，ID: " + id)); //[cite: 16]
        return convertToDTO(purchaseOrder);
    }

    // 4. 新增進貨單 (增加庫存 + 更新最後進價與移動平均成本)[cite: 16]
    @Override
    @Transactional(rollbackFor = Exception.class) //[cite: 16]
    public PurchaseOrderDTO createPurchaseOrder(CreatePurchaseOrderDTO createDTO) {
        if (createDTO.getSupplierId() != null && !supplierRepository.existsById(createDTO.getSupplierId())) {
            throw new RuntimeException("找不到對應的廠商 ID: " + createDTO.getSupplierId()); //[cite: 16]
        }

        PurchaseOrder purchaseOrder = new PurchaseOrder();
        purchaseOrder.setPurchaseNo(generatePurchaseNo()); //[cite: 16]
        purchaseOrder.setSupplierId(createDTO.getSupplierId()); //[cite: 16]
        purchaseOrder.setPurchaseDate(createDTO.getPurchaseDate() != null ? createDTO.getPurchaseDate() : LocalDate.now());
        purchaseOrder.setRemark(createDTO.getRemark()); //[cite: 16]
        purchaseOrder.setDiscountAmount(createDTO.getDiscountAmount() != null ? createDTO.getDiscountAmount() : BigDecimal.ZERO);

        BigDecimal grandTotal = BigDecimal.ZERO;

        if (createDTO.getItems() != null) {
            for (CreatePurchaseOrderDTO.CreateItemDTO itemDTO : createDTO.getItems()) {
                Product product = productRepository.findById(itemDTO.getProductId())
                        .orElseThrow(() -> new RuntimeException("商品不存在，ID: " + itemDTO.getProductId())); //[cite: 16]

                BigDecimal inQty = itemDTO.getQuantity() != null ? itemDTO.getQuantity() : BigDecimal.ZERO; //[cite: 16]
                BigDecimal currentStock = product.getStockQuantity() != null ? product.getStockQuantity() : BigDecimal.ZERO; //[cite: 16]

                // 決定進貨單價
                BigDecimal unitPrice = itemDTO.getUnitPrice(); //[cite: 16]
                if (unitPrice == null) {
                    unitPrice = product.getCostPrice() != null ? product.getCostPrice() : BigDecimal.ZERO; //[cite: 16]
                }

                // 💡 三軌成本自動計算與更新：
                // 1) 更新最後進價 (lastCostPrice)
                product.setLastCostPrice(unitPrice);

                // 2) 計算加權移動平均成本: (舊庫存 * 舊平均成本 + 新進貨量 * 新單價) / (舊庫存 + 新進貨量)
                BigDecimal oldAvgCost = product.getAvgCostPrice() != null ? product.getAvgCostPrice() : (product.getCostPrice() != null ? product.getCostPrice() : BigDecimal.ZERO);
                BigDecimal oldTotalVal = currentStock.multiply(oldAvgCost);
                BigDecimal newInVal = inQty.multiply(unitPrice);
                BigDecimal newTotalQty = currentStock.add(inQty);

                if (newTotalQty.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal newAvgCost = oldTotalVal.add(newInVal).divide(newTotalQty, 2, RoundingMode.HALF_UP);
                    product.setAvgCostPrice(newAvgCost);
                }

                // 3) 自動增加庫存[cite: 16]
                product.setStockQuantity(newTotalQty);
                productRepository.save(product); //[cite: 16]

                // 建立明細項[cite: 16]
                PurchaseOrderItem orderItem = new PurchaseOrderItem();
                orderItem.setProductId(product.getId()); //[cite: 16]
                orderItem.setProductName(product.getProductName()); //[cite: 16]
                orderItem.setProductCode(product.getProductCode());
                orderItem.setQuantity(inQty); //[cite: 16]
                orderItem.setUnitPrice(unitPrice); //[cite: 16]

                BigDecimal subtotal = unitPrice.multiply(inQty); //[cite: 16]
                orderItem.setSubtotal(subtotal); //[cite: 16]

                grandTotal = grandTotal.add(subtotal); //[cite: 16]
                purchaseOrder.addItem(orderItem); //[cite: 16]
            }
        }

        BigDecimal finalTotal = grandTotal.subtract(purchaseOrder.getDiscountAmount());
        purchaseOrder.setTotalAmount(finalTotal.compareTo(BigDecimal.ZERO) > 0 ? finalTotal : BigDecimal.ZERO);

        PurchaseOrder savedOrder = purchaseOrderRepository.save(purchaseOrder); //[cite: 16]
        return convertToDTO(savedOrder);
    }

    // 5. 修改進貨單 (先扣回舊數量，再加回新數量)
    @Override
    @Transactional(rollbackFor = Exception.class)
    public PurchaseOrderDTO updatePurchaseOrder(Long id, CreatePurchaseOrderDTO updateDTO) {
        PurchaseOrder existingOrder = purchaseOrderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("找不到該進貨單，無法更新！ID: " + id));

        // 步驟 A：將舊進貨單增加的庫存扣回
        if (existingOrder.getItems() != null) {
            for (PurchaseOrderItem oldItem : existingOrder.getItems()) {
                Product product = productRepository.findById(oldItem.getProductId()).orElse(null);
                if (product != null) {
                    BigDecimal oldQty = oldItem.getQuantity() != null ? oldItem.getQuantity() : BigDecimal.ZERO;
                    product.setStockQuantity((product.getStockQuantity() != null ? product.getStockQuantity() : BigDecimal.ZERO).subtract(oldQty));
                    productRepository.save(product);
                }
            }
            existingOrder.getItems().clear();
        }

        // 步驟 B：更新主檔資訊
        existingOrder.setSupplierId(updateDTO.getSupplierId());
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
                orderItem.setProductId(product.getId());
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

    // 6. 作廢/刪除進貨單 (庫存自動扣回)
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePurchaseOrder(Long id) {
        PurchaseOrder purchaseOrder = purchaseOrderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("找不到進貨單，無法作廢！ID: " + id));

        if (purchaseOrder.getItems() != null) {
            for (PurchaseOrderItem item : purchaseOrder.getItems()) {
                Product product = productRepository.findById(item.getProductId()).orElse(null);
                if (product != null) {
                    BigDecimal currentStock = product.getStockQuantity() != null ? product.getStockQuantity() : BigDecimal.ZERO;
                    BigDecimal itemQty = item.getQuantity() != null ? item.getQuantity() : BigDecimal.ZERO;
                    product.setStockQuantity(currentStock.subtract(itemQty)); // 把當初增加的扣回
                    productRepository.save(product);
                }
            }
        }

        purchaseOrderRepository.delete(purchaseOrder);
    }

    private String generatePurchaseNo() {
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")); //[cite: 16]
        String randomStr = UUID.randomUUID().toString().substring(0, 4).toUpperCase(); //[cite: 16]
        return "PO-" + dateStr + "-" + randomStr; //[cite: 16]
    }

    private PurchaseOrderDTO convertToDTO(PurchaseOrder entity) {
        PurchaseOrderDTO dto = new PurchaseOrderDTO(); //[cite: 16]
        dto.setId(entity.getId()); //[cite: 16]
        dto.setPurchaseNo(entity.getPurchaseNo()); //[cite: 16]
        dto.setSupplierId(entity.getSupplierId()); //[cite: 16]
        dto.setPurchaseDate(entity.getPurchaseDate()); //[cite: 16]
        dto.setRemark(entity.getRemark()); //[cite: 16]
        dto.setDiscountAmount(entity.getDiscountAmount());
        dto.setTotalAmount(entity.getTotalAmount()); //[cite: 16]

        if (entity.getSupplierId() != null) {
            supplierRepository.findById(entity.getSupplierId()).ifPresent(s -> {
                dto.setSupplierName(s.getShortName() != null ? s.getShortName() : s.getFullName());
            });
        }

        List<PurchaseOrderDTO.ItemDTO> itemDTOs = new ArrayList<>(); //[cite: 16]
        if (entity.getItems() != null) { //[cite: 16]
            for (PurchaseOrderItem item : entity.getItems()) { //[cite: 16]
                PurchaseOrderDTO.ItemDTO itemDTO = new PurchaseOrderDTO.ItemDTO(); //[cite: 16]
                itemDTO.setId(item.getId()); //[cite: 16]
                itemDTO.setProductId(item.getProductId()); //[cite: 16]
                itemDTO.setProductName(item.getProductName()); //[cite: 16]
                itemDTO.setProductCode(item.getProductCode());
                itemDTO.setQuantity(item.getQuantity()); //[cite: 16]
                itemDTO.setUnitPrice(item.getUnitPrice()); //[cite: 16]
                itemDTO.setSubtotal(item.getSubtotal()); //[cite: 16]
                itemDTOs.add(itemDTO); //[cite: 16]
            }
        }
        dto.setItems(itemDTOs); //[cite: 16]
        return dto; //[cite: 16]
    }
}