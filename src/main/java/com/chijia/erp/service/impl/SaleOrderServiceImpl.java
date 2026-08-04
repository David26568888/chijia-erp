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

import com.chijia.erp.model.dto.CreateSaleOrderDTO;
import com.chijia.erp.model.dto.SaleOrderDTO;
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

    @Override
    @Transactional(rollbackFor = Exception.class) // 💡 確保庫存扣減與銷貨單寫入在同一事務中
    public SaleOrderDTO createSaleOrder(CreateSaleOrderDTO createDTO) {

        // 1. 驗證客戶是否存在 (若有帶入 customerId)
        if (createDTO.getCustomerId() != null && !customerRepository.existsById(createDTO.getCustomerId())) {
            throw new RuntimeException("找不到對應的客戶 ID: " + createDTO.getCustomerId());
        }

        // 2. 建立銷貨單主檔
        SaleOrder saleOrder = new SaleOrder();
        saleOrder.setSaleNo(generateSaleNo()); // 自動生成單號 SO-YYYYMMDD-XXXX
        saleOrder.setCustomerId(createDTO.getCustomerId());
        saleOrder.setRemark(createDTO.getRemark());

        BigDecimal grandTotal = BigDecimal.ZERO;

     // 3. 處理每筆銷貨明細 & 自動扣減庫存
        if (createDTO.getItems() != null) {
            for (CreateSaleOrderDTO.CreateItemDTO itemDTO : createDTO.getItems()) {

                // 3a. 檢查銷貨商品是否存在
                Product product = productRepository.findById(itemDTO.getProductId())
                        .orElseThrow(() -> new RuntimeException("商品不存在，ID: " + itemDTO.getProductId()));

                BigDecimal sellQty = itemDTO.getQuantity() != null ? itemDTO.getQuantity() : BigDecimal.ZERO;
                BigDecimal currentStock = product.getStockQuantity() != null ? product.getStockQuantity() : BigDecimal.ZERO;

                // 3b. 💡 實務調整：允許負庫存銷售 (取消 throw Exception，改為後台警示 Log)
                if (sellQty.compareTo(BigDecimal.ZERO) > 0 && currentStock.compareTo(sellQty) < 0) {
                    // 僅印出 Log 警示，不中斷交易，讓店員順利開單！
                    System.out.println("⚠️ [庫存警示] 商品 [" + product.getProductName() 
                            + "] 庫存不足！目前庫存: " + currentStock 
                            + "，銷售數量: " + sellQty + "，交易後將呈現負庫存！");
                }

                // 3c. 💡 自動扣減庫存 (使用 .subtract())
                // 註：若 sellQty 為負數 (例如 -2 銷退)，subtract(-2) 會自動變成 +2，庫存精準回補！
                product.setStockQuantity(currentStock.subtract(sellQty));
                productRepository.save(product); // 更新商品庫存

                // 3d. 建立銷貨明細 Entity
                SaleOrderItem orderItem = new SaleOrderItem();
                orderItem.setProductId(product.getId());
                orderItem.setProductName(product.getProductName());
                orderItem.setQuantity(sellQty);

                // 3e. 決定銷售單價：若有傳入則以此為主，否則自動抓取商品預設售價 (salePrice)
                BigDecimal unitPrice = itemDTO.getUnitPrice();
                if (unitPrice == null) {
                    unitPrice = product.getSalePrice() != null ? product.getSalePrice() : BigDecimal.ZERO;
                }
                orderItem.setUnitPrice(unitPrice);

                // 3f. 💡 計算小計金額使用 .multiply()：單價 * 數量 (數量為負數時，小計也會自動為負數)
                BigDecimal subtotal = unitPrice.multiply(sellQty);
                orderItem.setSubtotal(subtotal);

                // 累加整張銷貨單總金額
                grandTotal = grandTotal.add(subtotal);

                // 雙向關聯加入主檔
                saleOrder.addItem(orderItem);
            }
        }

        saleOrder.setTotalAmount(grandTotal);

        // 4. 存入資料庫 (連同明細一同寫入)
        SaleOrder savedOrder = saleOrderRepository.save(saleOrder);

        // 5. 轉為 SaleOrderDTO 回傳
        return convertToDTO(savedOrder);
    }

    @Override
    public SaleOrderDTO getSaleOrderById(Long id) {
        SaleOrder saleOrder = saleOrderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("找不到銷貨單，ID: " + id));
        return convertToDTO(saleOrder);
    }

    // 💡 自動生成銷貨單號邏輯 (格式：SO-YYYYMMDD-隨機4碼)
    private String generateSaleNo() {
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomStr = UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        return "SO-" + dateStr + "-" + randomStr;
    }

    // Entity 轉 DTO 轉換邏輯
    private SaleOrderDTO convertToDTO(SaleOrder entity) {
        SaleOrderDTO dto = new SaleOrderDTO();
        dto.setId(entity.getId());
        dto.setSaleNo(entity.getSaleNo());
        dto.setCustomerId(entity.getCustomerId());
        dto.setTotalAmount(entity.getTotalAmount());
        dto.setSaleDate(entity.getSaleDate());
        dto.setRemark(entity.getRemark());

        List<SaleOrderDTO.ItemDTO> itemDTOs = new ArrayList<>();
        if (entity.getItems() != null) {
            for (SaleOrderItem item : entity.getItems()) {
                SaleOrderDTO.ItemDTO itemDTO = new SaleOrderDTO.ItemDTO();
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