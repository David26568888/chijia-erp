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
import org.springframework.data.domain.PageRequest;
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
import com.chijia.erp.repository.SaleOrderItemRepository;
import com.chijia.erp.repository.SaleOrderRepository;
import com.chijia.erp.service.SaleOrderService;

@Service
public class SaleOrderServiceImpl implements SaleOrderService {

    @Autowired
    private SaleOrderRepository saleOrderRepository;

    @Autowired
    private SaleOrderItemRepository saleOrderItemRepository;

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
                        
                        // 💡 採用 JPA 物件關聯：直接透過 o.getCustomer() 取得客戶名稱
                        String custName = "";
                        if (o.getCustomer() != null) {
                            custName = o.getCustomer().getShortName() != null ? 
                                       o.getCustomer().getShortName() : o.getCustomer().getFullName();
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
        if (id == null) {
            throw new IllegalArgumentException("查詢失敗：銷貨單 ID 不能為空！");
        }
        SaleOrder saleOrder = saleOrderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("找不到銷貨單，ID: " + id));
        return convertToDTO(saleOrder);
    }

    // 4. 新增銷貨單 (支援 deductStock 開關，並自動計算成本與毛利)
    @Override
    @Transactional(rollbackFor = Exception.class)
    public SaleOrderDTO createSaleOrder(CreateSaleOrderDTO createDTO) {
        SaleOrder saleOrder = new SaleOrder();
        saleOrder.setSaleNo(generateSaleNo()); 
        
        // 💡 採用 JPA 物件關聯設定 Customer
        if (createDTO.getCustomerId() != null) {
            Customer customer = customerRepository.findById(createDTO.getCustomerId())
                    .orElseThrow(() -> new RuntimeException("找不到對應的客戶 ID: " + createDTO.getCustomerId()));
            saleOrder.setCustomer(customer);
        }

        saleOrder.setSaleDate(createDTO.getSaleDate() != null ? createDTO.getSaleDate() : LocalDate.now());
        saleOrder.setRemark(createDTO.getRemark()); 
        saleOrder.setDiscountAmount(createDTO.getDiscountAmount() != null ? createDTO.getDiscountAmount() : BigDecimal.ZERO);

        BigDecimal grandTotal = BigDecimal.ZERO;

        if (createDTO.getItems() != null) {
            for (CreateSaleOrderDTO.CreateItemDTO itemDTO : createDTO.getItems()) {
                if (itemDTO == null || itemDTO.getProductId() == null) {
                    continue;
                }
                
                Product product = productRepository.findById(itemDTO.getProductId())
                        .orElseThrow(() -> new RuntimeException("商品不存在，ID: " + itemDTO.getProductId()));

                BigDecimal sellQty = itemDTO.getQuantity() != null ? itemDTO.getQuantity() : BigDecimal.ZERO;
                
                if (createDTO.isDeductStock()) {
                    BigDecimal currentStock = product.getStockQuantity() != null ? product.getStockQuantity() : BigDecimal.ZERO;
                    product.setStockQuantity(currentStock.subtract(sellQty));
                    productRepository.save(product);
                }

                SaleOrderItem orderItem = new SaleOrderItem();
                orderItem.setProductId(product.getId()); 
                orderItem.setProductName(product.getProductName()); 
                orderItem.setProductCode(product.getProductCode());
                orderItem.setQuantity(sellQty); 

                BigDecimal unitPrice = itemDTO.getUnitPrice();
                if (unitPrice == null) {
                    unitPrice = product.getSalePrice() != null ? product.getSalePrice() : BigDecimal.ZERO;
                }
                orderItem.setUnitPrice(unitPrice); 

                BigDecimal subtotal = unitPrice.multiply(sellQty); 
                orderItem.setSubtotal(subtotal); 

                BigDecimal unitCost = BigDecimal.ZERO;
                if (product.getAvgCostPrice() != null && product.getAvgCostPrice().compareTo(BigDecimal.ZERO) > 0) {
                    unitCost = product.getAvgCostPrice();
                } else if (product.getCostPrice() != null) {
                    unitCost = product.getCostPrice();
                }
                
                orderItem.setCostPrice(unitCost);
                
                BigDecimal totalCost = unitCost.multiply(sellQty);
                orderItem.setTotalCost(totalCost);

                BigDecimal profit = subtotal.subtract(totalCost);
                orderItem.setGrossProfit(profit);

                grandTotal = grandTotal.add(subtotal); 
                saleOrder.addItem(orderItem);
            }
        }

        BigDecimal finalPay = grandTotal.subtract(saleOrder.getDiscountAmount());
        saleOrder.setTotalAmount(finalPay.compareTo(BigDecimal.ZERO) > 0 ? finalPay : BigDecimal.ZERO);

        SaleOrder savedOrder = saleOrderRepository.save(saleOrder);
        return convertToDTO(savedOrder);
    }

    // 5. 修改銷貨單
    @Override
    @Transactional(rollbackFor = Exception.class)
    public SaleOrderDTO updateSaleOrder(Long id, CreateSaleOrderDTO updateDTO) {
        if (id == null) {
            throw new IllegalArgumentException("更新失敗：銷貨單 ID 不能為空！");
        }
        
        SaleOrder existingOrder = saleOrderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("找不到該銷貨單，無法更新！ID: " + id));

        // 步驟 A：庫存回補
        if (existingOrder.getItems() != null) {
            for (SaleOrderItem oldItem : existingOrder.getItems()) {
                if (oldItem.getProductId() != null) {
                    Product product = productRepository.findById(oldItem.getProductId()).orElse(null);
                    if (product != null) {
                        BigDecimal oldQty = oldItem.getQuantity() != null ? oldItem.getQuantity() : BigDecimal.ZERO;
                        product.setStockQuantity((product.getStockQuantity() != null ? product.getStockQuantity() : BigDecimal.ZERO).add(oldQty));
                        productRepository.save(product);
                    }
                }
            }
            existingOrder.getItems().clear();
        }

        // 步驟 B：更新主檔資訊 (帶入 Customer 物件)
        if (updateDTO.getCustomerId() != null) {
            Customer customer = customerRepository.findById(updateDTO.getCustomerId())
                    .orElseThrow(() -> new RuntimeException("找不到對應的客戶 ID: " + updateDTO.getCustomerId()));
            existingOrder.setCustomer(customer);
        } else {
            existingOrder.setCustomer(null);
        }

        if (updateDTO.getSaleDate() != null) {
            existingOrder.setSaleDate(updateDTO.getSaleDate());
        }
        existingOrder.setRemark(updateDTO.getRemark());
        existingOrder.setDiscountAmount(updateDTO.getDiscountAmount() != null ? updateDTO.getDiscountAmount() : BigDecimal.ZERO);

        // 步驟 C：重新扣庫存與計算明細
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

                BigDecimal unitCost = BigDecimal.ZERO;
                if (product.getAvgCostPrice() != null && product.getAvgCostPrice().compareTo(BigDecimal.ZERO) > 0) {
                    unitCost = product.getAvgCostPrice();
                } else if (product.getCostPrice() != null) {
                    unitCost = product.getCostPrice();
                }
                
                orderItem.setCostPrice(unitCost);
                
                BigDecimal totalCost = unitCost.multiply(sellQty);
                orderItem.setTotalCost(totalCost);

                BigDecimal profit = subtotal.subtract(totalCost);
                orderItem.setGrossProfit(profit);

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

    // 7. 建議售價查詢 (銷售彈窗選商品時觸發)
    @Override
    @Transactional(readOnly = true)
    public BigDecimal getSuggestedPrice(Long customerId, Long productId) {
        if (productId == null) {
            return BigDecimal.ZERO;
        }

        // 💡 對應 SaleOrderItemRepository 新的 JPQL 方法，取最新的第一筆
        if (customerId != null) {
            List<SaleOrderItem> recentItems = saleOrderItemRepository
                    .findRecentPriceByCustomerAndProduct(customerId, productId, PageRequest.of(0, 1));

            if (!recentItems.isEmpty() && recentItems.get(0).getUnitPrice() != null) {
                return recentItems.get(0).getUnitPrice();
            }
        }

        return productRepository.findById(productId)
                .map(p -> p.getSalePrice() != null ? p.getSalePrice() : BigDecimal.ZERO)
                .orElse(BigDecimal.ZERO);
    }

    private String generateSaleNo() {
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomStr = UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        return "SO-" + dateStr + "-" + randomStr;
    }

    private SaleOrderDTO convertToDTO(SaleOrder entity) {
        SaleOrderDTO dto = new SaleOrderDTO();
        dto.setId(entity.getId());
        dto.setSaleNo(entity.getSaleNo());
        dto.setSaleDate(entity.getSaleDate());
        dto.setRemark(entity.getRemark());
        dto.setDiscountAmount(entity.getDiscountAmount());
        dto.setTotalAmount(entity.getTotalAmount());

        if (entity.getCustomer() != null) {
            dto.setCustomerId(entity.getCustomer().getId());
            String custName = entity.getCustomer().getShortName() != null ? 
                              entity.getCustomer().getShortName() : entity.getCustomer().getFullName();
            dto.setCustomerName(custName);
        }

        List<SaleOrderDTO.ItemDTO> itemDTOs = new ArrayList<>();
        if (entity.getItems() != null) {
            for (SaleOrderItem item : entity.getItems()) {
                SaleOrderDTO.ItemDTO itemDTO = new SaleOrderDTO.ItemDTO();
                itemDTO.setId(item.getId());
                itemDTO.setProductId(item.getProductId());
                itemDTO.setProductName(item.getProductName());
                itemDTO.setProductCode(item.getProductCode());
                itemDTO.setQuantity(item.getQuantity());
                itemDTO.setUnitPrice(item.getUnitPrice());
                itemDTO.setSubtotal(item.getSubtotal());
                
                itemDTO.setCostPrice(item.getCostPrice());
                itemDTO.setTotalCost(item.getTotalCost());
                itemDTO.setGrossProfit(item.getGrossProfit());

                itemDTOs.add(itemDTO);
            }
        }
        dto.setItems(itemDTOs);
        return dto;
    }
}