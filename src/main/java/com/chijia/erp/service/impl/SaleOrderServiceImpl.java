package com.chijia.erp.service.impl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Pageable;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

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
import com.chijia.erp.util.ExcelHelper;

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

 // 4. 新增銷貨單 (支援 deductStock 開關，自動帶入歷史成交價並計算主表成本與毛利)
    @Override
    @Transactional(rollbackFor = Exception.class)
    public SaleOrderDTO createSaleOrder(CreateSaleOrderDTO createDTO) {
        SaleOrder saleOrder = new SaleOrder();

        // 1. 綁定客戶
        if (createDTO.getCustomerId() != null) {
            Customer customer = customerRepository.findById(createDTO.getCustomerId())
                    .orElseThrow(() -> new RuntimeException("找不到對應的客戶 ID: " + createDTO.getCustomerId()));
            saleOrder.setCustomer(customer);
        }

        saleOrder.setSaleNo(generateSaleNo());
        saleOrder.setSaleDate(createDTO.getSaleDate() != null ? createDTO.getSaleDate() : LocalDate.now());
        saleOrder.setRemark(createDTO.getRemark());

        BigDecimal discountAmount = createDTO.getDiscountAmount() != null ? createDTO.getDiscountAmount() : BigDecimal.ZERO;
        saleOrder.setDiscountAmount(discountAmount);

        BigDecimal grandTotal = BigDecimal.ZERO;     // 全單未折扣總金額
        BigDecimal orderTotalCost = BigDecimal.ZERO; // 全單總成本

        if (createDTO.getItems() != null && !createDTO.getItems().isEmpty()) {
            for (CreateSaleOrderDTO.CreateItemDTO itemDTO : createDTO.getItems()) {
                if (itemDTO == null || itemDTO.getProductId() == null) {
                    continue;
                }

                Product product = productRepository.findById(itemDTO.getProductId())
                        .orElseThrow(() -> new RuntimeException("商品不存在，ID: " + itemDTO.getProductId()));

                BigDecimal sellQty = itemDTO.getQuantity() != null ? itemDTO.getQuantity() : BigDecimal.ZERO;
                if (sellQty.compareTo(BigDecimal.ZERO) <= 0) {
                    continue; // 忽略數量 <= 0 的明細
                }

                // 2. 扣減庫存 logic
                if (createDTO.isDeductStock()) {
                    BigDecimal currentStock = product.getStockQuantity() != null ? product.getStockQuantity() : BigDecimal.ZERO;
                    product.setStockQuantity(currentStock.subtract(sellQty));
                    productRepository.save(product);
                }

                SaleOrderItem orderItem = new SaleOrderItem();
                // 💡 寫入關鍵 ID 與快照，避免外鍵遺失
                orderItem.setProductId(product.getId());
                orderItem.setProductCode(product.getProductCode());
                orderItem.setProductName(product.getProductName());
                orderItem.setQuantity(sellQty);

                // 3. 確定銷售單價：前端指定單價 -> 客戶歷史成交價 -> 商品預設售價
                BigDecimal unitPrice = itemDTO.getUnitPrice();
                if (unitPrice == null) {
                    unitPrice = resolveProductUnitPrice(createDTO.getCustomerId(), product);
                }
                orderItem.setUnitPrice(unitPrice);

                // 4. 計算項目小計
                BigDecimal subtotal = unitPrice.multiply(sellQty);
                orderItem.setSubtotal(subtotal);

                // 5. 成本與毛利計算 (優先使用平均成本，其次使用進價)
                BigDecimal unitCost = BigDecimal.ZERO;
                if (product.getAvgCostPrice() != null && product.getAvgCostPrice().compareTo(BigDecimal.ZERO) > 0) {
                    unitCost = product.getAvgCostPrice();
                } else if (product.getCostPrice() != null) {
                    unitCost = product.getCostPrice();
                }

                orderItem.setCostPrice(unitCost);

                BigDecimal totalCost = unitCost.multiply(sellQty);
                orderItem.setTotalCost(totalCost);

                BigDecimal itemProfit = subtotal.subtract(totalCost);
                orderItem.setGrossProfit(itemProfit);

                // 累加全單數值
                grandTotal = grandTotal.add(subtotal);
                orderTotalCost = orderTotalCost.add(totalCost);

                // 6. 雙向關聯綁定 (addItem 內部應自動做 orderItem.setSaleOrder(this))
                saleOrder.addItem(orderItem);
            }
        }

        // 7. 計算主表應收總額與整單總毛利
        BigDecimal finalPay = grandTotal.subtract(discountAmount);
        saleOrder.setTotalAmount(finalPay.compareTo(BigDecimal.ZERO) > 0 ? finalPay : BigDecimal.ZERO);
        saleOrder.setTotalCost(orderTotalCost);

        // 主表整單毛利 = 最終應收金額 - 總成本 (自動扣除折讓)
        BigDecimal orderGrossProfit = saleOrder.getTotalAmount().subtract(orderTotalCost);
        saleOrder.setGrossProfit(orderGrossProfit);

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
    
    
    /**
     * 💡 私有輔助方法：解析商品銷售單價
     */
    
    private BigDecimal resolveProductUnitPrice(Long customerId, Product product) {
    	if (customerId != null) {
            Pageable pageable = PageRequest.of(0, 1);
            List<SaleOrderItem> historyItems = saleOrderItemRepository
                    .findRecentPriceByCustomerAndProduct(customerId, product.getId(), pageable);
            if (!historyItems.isEmpty() && historyItems.get(0).getUnitPrice() != null) {
                return historyItems.get(0).getUnitPrice();
            }
        }
        return product.getSalePrice() != null ? product.getSalePrice() : BigDecimal.ZERO;
    }

 // --- 匯入 Excel ---
    @Override
    @Transactional(rollbackFor = Exception.class)
    public String importSaleOrdersFromExcel(MultipartFile file, boolean deductStock) {
        try (InputStream is = file.getInputStream();
             Workbook workbook = WorkbookFactory.create(is)) {

            Sheet sheet = workbook.getSheetAt(0);
            Map<String, CreateSaleOrderDTO> orderMap = new LinkedHashMap<>();

            Map<String, Product> productCodeMap = new HashMap<>();
            productRepository.findAll().forEach(p -> {
                if (p.getProductCode() != null) productCodeMap.put(p.getProductCode().trim(), p);
            });

            Map<String, Customer> customerCodeMap = new HashMap<>();
            customerRepository.findAll().forEach(c -> {
                if (c.getCustomerCode() != null) customerCodeMap.put(c.getCustomerCode().trim(), c);
            });

            for (int r = 7; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;

                String docType = ExcelHelper.getCellValueAsString(row.getCell(0));
                String saleNo = ExcelHelper.getCellValueAsString(row.getCell(1));
                String dateStr = ExcelHelper.getCellValueAsString(row.getCell(2));
                String custCode = ExcelHelper.getCellValueAsString(row.getCell(3));
                String productCode = ExcelHelper.getCellValueAsString(row.getCell(7));

                BigDecimal qty = ExcelHelper.getCellValueAsBigDecimal(row.getCell(10), BigDecimal.ZERO);
                BigDecimal price = ExcelHelper.getCellValueAsBigDecimal(row.getCell(11), BigDecimal.ZERO);

                if (saleNo.isEmpty() || productCode.isEmpty() || (!"銷貨".equals(docType) && !"銷退".equals(docType))) {
                    continue;
                }

                Product product = productCodeMap.get(productCode.trim());
                if (product == null) continue;

                Customer customer = customerCodeMap.get(custCode.trim());
                Long customerId = customer != null ? customer.getId() : null;

                if ("銷退".equals(docType) && qty.compareTo(BigDecimal.ZERO) > 0) {
                    qty = qty.negate();
                }

                CreateSaleOrderDTO orderDTO = orderMap.computeIfAbsent(saleNo, k -> {
                    CreateSaleOrderDTO dto = new CreateSaleOrderDTO();
                    dto.setCustomerId(customerId);
                    dto.setSaleDate(parseMinguoDate(dateStr));
                    dto.setRemark("舊ERP銷貨單 [" + k + "]");
                    dto.setDeductStock(deductStock);
                    dto.setDiscountAmount(BigDecimal.ZERO);
                    dto.setItems(new ArrayList<>());
                    return dto;
                });

                CreateSaleOrderDTO.CreateItemDTO itemDTO = new CreateSaleOrderDTO.CreateItemDTO();
                itemDTO.setProductId(product.getId());
                itemDTO.setQuantity(qty);
                itemDTO.setUnitPrice(price);
                orderDTO.getItems().add(itemDTO);
            }

            int count = 0;
            for (CreateSaleOrderDTO dto : orderMap.values()) {
                if (!dto.getItems().isEmpty()) {
                    createSaleOrder(dto);
                    count++;
                }
            }

            return "成功匯入 " + count + " 筆舊銷貨單！";

        } catch (Exception e) {
            throw new RuntimeException("銷貨紀錄 Excel 匯入失敗：" + e.getMessage(), e);
        }
    }

 // --- 匯出 Excel ---
    @Override
    public byte[] exportSaleOrdersToExcel() throws IOException {
        List<SaleOrder> orders = saleOrderRepository.findAll();

        try (Workbook workbook = new XSSFWorkbook(); 
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            
            Sheet sheet = workbook.createSheet("銷貨紀錄");

            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.ROYAL_BLUE.getIndex()); 
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            Row headerRow = sheet.createRow(0);
            String[] headers = {"銷貨單號", "銷貨日期", "客戶名稱", "折讓金額", "實收總金額", "備註"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowNum = 1;
            for (SaleOrder order : orders) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(order.getSaleNo());
                row.createCell(1).setCellValue(order.getSaleDate() != null ? order.getSaleDate().toString() : "");
                row.createCell(2).setCellValue(order.getCustomer() != null ? order.getCustomer().getShortName() : "散客");
                row.createCell(3).setCellValue(order.getDiscountAmount() != null ? order.getDiscountAmount().doubleValue() : 0.0);
                row.createCell(4).setCellValue(order.getTotalAmount() != null ? order.getTotalAmount().doubleValue() : 0.0);
                row.createCell(5).setCellValue(order.getRemark() != null ? order.getRemark() : "");
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        }
    }

    private LocalDate parseMinguoDate(String minguoStr) {
        if (minguoStr == null || !minguoStr.contains("/")) return LocalDate.now();
        try {
            String[] parts = minguoStr.split("/");
            int year = Integer.parseInt(parts[0].trim()) + 1911;
            int month = Integer.parseInt(parts[1].trim());
            int day = Integer.parseInt(parts[2].trim());
            return LocalDate.of(year, month, day);
        } catch (Exception e) {
            return LocalDate.now();
        }
    }

    
}
