package com.chijia.erp.service.impl;

import java.math.BigDecimal;
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

import com.chijia.erp.model.dto.CreateSaleOrderDTO;
import com.chijia.erp.model.dto.SaleOrderDTO;
import com.chijia.erp.model.entity.Customer;
import com.chijia.erp.model.entity.Product;
import com.chijia.erp.model.entity.SaleOrder;
import com.chijia.erp.model.entity.SaleOrderItem;
import com.chijia.erp.repository.CustomerRepository;
import com.chijia.erp.repository.ProductRepository;
import com.chijia.erp.repository.SaleOrderRepository;
import com.chijia.erp.service.SaleOrderService;

@Service
public class SaleOrderServiceImpl implements SaleOrderService {

    @Autowired
    private SaleOrderRepository saleOrderRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CustomerRepository customerRepository;

    // 1. 查詢所有銷貨單 (按 ID 倒序，最新開單的排最前面)
    @Override
    public List<SaleOrderDTO> getAllSaleOrders() {
        return saleOrderRepository.findAll().stream()
                .sorted((a, b) -> b.getId().compareTo(a.getId()))
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // 2. 多條件搜尋銷貨單
    @Override
    public List<SaleOrderDTO> searchSaleOrders(String keyword, LocalDate startDate, LocalDate endDate) {
        List<SaleOrder> orders = saleOrderRepository.findAll();

        return orders.stream()
                .filter(o -> {
                    boolean matchKw = true;
                    if (keyword != null && !keyword.trim().isEmpty()) {
                        String kw = keyword.trim().toLowerCase();
                        boolean matchNo = o.getSaleNo() != null && o.getSaleNo().toLowerCase().contains(kw);
                        
                        String custName = "";
                        if (o.getCustomerId() != null) {
                            custName = customerRepository.findById(o.getCustomerId())
                                    .map(Customer::getShortName).orElse("");
                        }
                        boolean matchCust = custName.toLowerCase().contains(kw);
                        matchKw = matchNo || matchCust;
                    }

                    boolean matchDate = true;
                    if (startDate != null && o.getSaleDate() != null) {
                        matchDate = !o.getSaleDate().isBefore(startDate);
                    }
                    if (endDate != null && o.getSaleDate() != null && matchDate) {
                        matchDate = !o.getSaleDate().isAfter(endDate);
                    }

                    return matchKw && matchDate;
                })
                .sorted((a, b) -> b.getId().compareTo(a.getId()))
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // 3. 依 ID 查詢單一銷貨單
    @Override
    public SaleOrderDTO getSaleOrderById(Long id) {
        SaleOrder saleOrder = saleOrderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("找不到銷貨單，ID: " + id)); //[cite: 8]
        return convertToDTO(saleOrder);
    }

    // 4. 新增銷貨單 (自動扣庫存)
    @Override
    @Transactional(rollbackFor = Exception.class) //[cite: 8]
    public SaleOrderDTO createSaleOrder(CreateSaleOrderDTO createDTO) {
        if (createDTO.getCustomerId() != null && !customerRepository.existsById(createDTO.getCustomerId())) {
            throw new RuntimeException("找不到對應的客戶 ID: " + createDTO.getCustomerId()); //[cite: 8]
        }

        SaleOrder saleOrder = new SaleOrder();
        saleOrder.setSaleNo(generateSaleNo()); //[cite: 8]
        saleOrder.setCustomerId(createDTO.getCustomerId()); //[cite: 8]
        saleOrder.setSaleDate(createDTO.getSaleDate() != null ? createDTO.getSaleDate() : LocalDate.now());
        saleOrder.setRemark(createDTO.getRemark()); //[cite: 8]
        saleOrder.setDiscountAmount(createDTO.getDiscountAmount() != null ? createDTO.getDiscountAmount() : BigDecimal.ZERO);

        BigDecimal grandTotal = BigDecimal.ZERO;

        if (createDTO.getItems() != null) {
            for (CreateSaleOrderDTO.CreateItemDTO itemDTO : createDTO.getItems()) {
                Product product = productRepository.findById(itemDTO.getProductId())
                        .orElseThrow(() -> new RuntimeException("商品不存在，ID: " + itemDTO.getProductId())); //[cite: 8]

                BigDecimal sellQty = itemDTO.getQuantity() != null ? itemDTO.getQuantity() : BigDecimal.ZERO; //[cite: 8]
                BigDecimal currentStock = product.getStockQuantity() != null ? product.getStockQuantity() : BigDecimal.ZERO; //[cite: 8]

                // 自動扣減庫存[cite: 8]
                product.setStockQuantity(currentStock.subtract(sellQty)); //[cite: 8]
                productRepository.save(product); //[cite: 8]

                // 建立明細項[cite: 8]
                SaleOrderItem orderItem = new SaleOrderItem();
                orderItem.setProductId(product.getId()); //[cite: 8]
                orderItem.setProductName(product.getProductName()); //[cite: 8]
                orderItem.setProductCode(product.getProductCode());
                orderItem.setQuantity(sellQty); //[cite: 8]

                BigDecimal unitPrice = itemDTO.getUnitPrice(); //[cite: 8]
                if (unitPrice == null) {
                    unitPrice = product.getSalePrice() != null ? product.getSalePrice() : BigDecimal.ZERO; //[cite: 8]
                }
                orderItem.setUnitPrice(unitPrice); //[cite: 8]

                BigDecimal subtotal = unitPrice.multiply(sellQty); //[cite: 8]
                orderItem.setSubtotal(subtotal); //[cite: 8]

                grandTotal = grandTotal.add(subtotal); //[cite: 8]
                saleOrder.addItem(orderItem); //[cite: 8]
            }
        }

        // 扣除整單折讓額
        BigDecimal finalPay = grandTotal.subtract(saleOrder.getDiscountAmount());
        saleOrder.setTotalAmount(finalPay.compareTo(BigDecimal.ZERO) > 0 ? finalPay : BigDecimal.ZERO);

        SaleOrder savedOrder = saleOrderRepository.save(saleOrder); //[cite: 8]
        return convertToDTO(savedOrder);
    }

    // 5. 修改銷貨單 (將原單據庫存先回補，再依新單據重新扣庫存)
    @Override
    @Transactional(rollbackFor = Exception.class)
    public SaleOrderDTO updateSaleOrder(Long id, CreateSaleOrderDTO updateDTO) {
        SaleOrder existingOrder = saleOrderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("找不到該銷貨單，無法更新！ID: " + id));

        // 步驟 A：將舊銷貨單的庫存全數加回
        if (existingOrder.getItems() != null) {
            for (SaleOrderItem oldItem : existingOrder.getItems()) {
                Product product = productRepository.findById(oldItem.getProductId()).orElse(null);
                if (product != null) {
                    BigDecimal oldQty = oldItem.getQuantity() != null ? oldItem.getQuantity() : BigDecimal.ZERO;
                    product.setStockQuantity((product.getStockQuantity() != null ? product.getStockQuantity() : BigDecimal.ZERO).add(oldQty));
                    productRepository.save(product);
                }
            }
            existingOrder.getItems().clear(); // 清空舊明細
        }

        // 步驟 B：更新主檔資訊
        existingOrder.setCustomerId(updateDTO.getCustomerId());
        if (updateDTO.getSaleDate() != null) {
            existingOrder.setSaleDate(updateDTO.getSaleDate());
        }
        existingOrder.setRemark(updateDTO.getRemark());
        existingOrder.setDiscountAmount(updateDTO.getDiscountAmount() != null ? updateDTO.getDiscountAmount() : BigDecimal.ZERO);

        // 步驟 C：重新計算新明細並扣除庫存
        BigDecimal grandTotal = BigDecimal.ZERO;

        if (updateDTO.getItems() != null) {
            for (CreateSaleOrderDTO.CreateItemDTO itemDTO : updateDTO.getItems()) {
                Product product = productRepository.findById(itemDTO.getProductId())
                        .orElseThrow(() -> new RuntimeException("商品不存在，ID: " + itemDTO.getProductId()));

                BigDecimal sellQty = itemDTO.getQuantity() != null ? itemDTO.getQuantity() : BigDecimal.ZERO;
                BigDecimal currentStock = product.getStockQuantity() != null ? product.getStockQuantity() : BigDecimal.ZERO;

                product.setStockQuantity(currentStock.subtract(sellQty));
                productRepository.save(product);

                SaleOrderItem orderItem = new SaleOrderItem();
                orderItem.setProductId(product.getId());
                orderItem.setProductName(product.getProductName());
                orderItem.setProductCode(product.getProductCode());
                orderItem.setQuantity(sellQty);

                BigDecimal unitPrice = itemDTO.getUnitPrice() != null ? itemDTO.getUnitPrice() : (product.getSalePrice() != null ? product.getSalePrice() : BigDecimal.ZERO);
                orderItem.setUnitPrice(unitPrice);

                BigDecimal subtotal = unitPrice.multiply(sellQty);
                orderItem.setSubtotal(subtotal);

                grandTotal = grandTotal.add(subtotal);
                existingOrder.addItem(orderItem);
            }
        }

        BigDecimal finalPay = grandTotal.subtract(existingOrder.getDiscountAmount());
        existingOrder.setTotalAmount(finalPay.compareTo(BigDecimal.ZERO) > 0 ? finalPay : BigDecimal.ZERO);

        SaleOrder updatedOrder = saleOrderRepository.save(existingOrder);
        return convertToDTO(updatedOrder);
    }

    // 6. 作廢/刪除銷貨單 (庫存 100% 自動回補)
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSaleOrder(Long id) {
        SaleOrder saleOrder = saleOrderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("找不到銷貨單，無法作廢！ID: " + id));

        // 遍歷所有商品明細，把當初扣除的庫存加回去
        if (saleOrder.getItems() != null) {
            for (SaleOrderItem item : saleOrder.getItems()) {
                Product product = productRepository.findById(item.getProductId()).orElse(null);
                if (product != null) {
                    BigDecimal currentStock = product.getStockQuantity() != null ? product.getStockQuantity() : BigDecimal.ZERO;
                    BigDecimal itemQty = item.getQuantity() != null ? item.getQuantity() : BigDecimal.ZERO;
                    product.setStockQuantity(currentStock.add(itemQty));
                    productRepository.save(product);
                }
            }
        }

        saleOrderRepository.delete(saleOrder);
    }

    private String generateSaleNo() {
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")); //[cite: 8]
        String randomStr = UUID.randomUUID().toString().substring(0, 4).toUpperCase(); //[cite: 8]
        return "SO-" + dateStr + "-" + randomStr; //[cite: 8]
    }

    private SaleOrderDTO convertToDTO(SaleOrder entity) {
        SaleOrderDTO dto = new SaleOrderDTO(); //[cite: 8]
        dto.setId(entity.getId()); //[cite: 8]
        dto.setSaleNo(entity.getSaleNo()); //[cite: 8]
        dto.setCustomerId(entity.getCustomerId()); //[cite: 8]
        dto.setSaleDate(entity.getSaleDate()); //[cite: 8]
        dto.setRemark(entity.getRemark()); //[cite: 8]
        dto.setDiscountAmount(entity.getDiscountAmount());
        dto.setTotalAmount(entity.getTotalAmount()); //[cite: 8]

        if (entity.getCustomerId() != null) {
            customerRepository.findById(entity.getCustomerId()).ifPresent(c -> {
                dto.setCustomerName(c.getShortName() != null ? c.getShortName() : c.getFullName());
            });
        }

        List<SaleOrderDTO.ItemDTO> itemDTOs = new ArrayList<>(); //[cite: 8]
        if (entity.getItems() != null) { //[cite: 8]
            for (SaleOrderItem item : entity.getItems()) { //[cite: 8]
                SaleOrderDTO.ItemDTO itemDTO = new SaleOrderDTO.ItemDTO(); //[cite: 8]
                itemDTO.setId(item.getId()); //[cite: 8]
                itemDTO.setProductId(item.getProductId()); //[cite: 8]
                itemDTO.setProductName(item.getProductName()); //[cite: 8]
                itemDTO.setProductCode(item.getProductCode());
                itemDTO.setQuantity(item.getQuantity()); //[cite: 8]
                itemDTO.setUnitPrice(item.getUnitPrice()); //[cite: 8]
                itemDTO.setSubtotal(item.getSubtotal()); //[cite: 8]
                itemDTOs.add(itemDTO); //[cite: 8]
            }
        }
        dto.setItems(itemDTOs); //[cite: 8]
        return dto; //[cite: 8]
    }
}