package com.chijia.erp.service.impl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

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
import com.chijia.erp.util.ExcelHelper;

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

 // --- 批次匯入歷史進貨 Excel ---
    @Override
    @Transactional(rollbackFor = Exception.class)
    public String importPurchaseOrdersFromExcel(MultipartFile file) {
        try (InputStream is = file.getInputStream();
             Workbook workbook = WorkbookFactory.create(is)) {

            Sheet sheet = workbook.getSheetAt(0);
            Map<String, CreatePurchaseOrderDTO> orderMap = new LinkedHashMap<>();

            // 建立記憶體快照比對，減少 DB 查詢次數
            Map<String, Product> productCodeMap = new HashMap<>();
            productRepository.findAll().forEach(p -> {
                if (p.getProductCode() != null) productCodeMap.put(p.getProductCode().trim(), p);
            });

            Map<String, Supplier> supplierCodeMap = new HashMap<>();
            supplierRepository.findAll().forEach(s -> {
                if (s.getSupplierCode() != null) supplierCodeMap.put(s.getSupplierCode().trim(), s);
            });

            // 從第 8 列 (索引 7) 開始讀取
            for (int r = 7; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;

                String docType = ExcelHelper.getCellValueAsString(row.getCell(0));     // 單據 (進貨/進退)
                String purchaseNo = ExcelHelper.getCellValueAsString(row.getCell(1));  // 單據憑證
                String dateStr = ExcelHelper.getCellValueAsString(row.getCell(2));     // 單據日期
                String supplierCode = ExcelHelper.getCellValueAsString(row.getCell(3));// 廠商編號
                String productCode = ExcelHelper.getCellValueAsString(row.getCell(7)); // 產品編號

                BigDecimal qty = ExcelHelper.getCellValueAsBigDecimal(row.getCell(10), BigDecimal.ZERO);
                BigDecimal price = ExcelHelper.getCellValueAsBigDecimal(row.getCell(11), BigDecimal.ZERO);

                // 過濾無效列
                if (purchaseNo.isEmpty() || productCode.isEmpty() || (!"進貨".equals(docType) && !"進退".equals(docType))) {
                    continue;
                }

                Product product = productCodeMap.get(productCode.trim());
                Supplier supplier = supplierCodeMap.get(supplierCode.trim());
                if (product == null || supplier == null) continue;

                // 進退數量轉負數
                if ("進退".equals(docType) && qty.compareTo(BigDecimal.ZERO) > 0) {
                    qty = qty.negate();
                }

                CreatePurchaseOrderDTO orderDTO = orderMap.computeIfAbsent(purchaseNo, k -> {
                    CreatePurchaseOrderDTO dto = new CreatePurchaseOrderDTO();
                    dto.setSupplierId(supplier.getId());
                    dto.setPurchaseDate(parseMinguoDate(dateStr));
                    dto.setRemark("舊ERP進貨單 [" + k + "]");
                    dto.setDiscountAmount(BigDecimal.ZERO);
                    dto.setItems(new ArrayList<>());
                    return dto;
                });

                CreatePurchaseOrderDTO.CreateItemDTO itemDTO = new CreatePurchaseOrderDTO.CreateItemDTO();
                itemDTO.setProductId(product.getId());
                itemDTO.setQuantity(qty);
                itemDTO.setUnitPrice(price);
                orderDTO.getItems().add(itemDTO);
            }

            int count = 0;
            for (CreatePurchaseOrderDTO dto : orderMap.values()) {
                if (!dto.getItems().isEmpty()) {
                    createPurchaseOrder(dto);
                    count++;
                }
            }

            return "成功匯入 " + count + " 筆舊進貨單並更新庫存！";

        } catch (Exception e) {
            throw new RuntimeException("進貨紀錄 Excel 匯入失敗：" + e.getMessage(), e);
        }
    }

    // --- 匯出進貨單 Excel 報表 ---
    @Override
    public byte[] exportPurchaseOrdersToExcel() throws IOException {
        List<PurchaseOrder> orders = purchaseOrderRepository.findAll();

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("進貨紀錄");

            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.DARK_TEAL.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            Row headerRow = sheet.createRow(0);
            String[] headers = {"進貨單號", "進貨日期", "廠商名稱", "折讓金額", "進貨總金額", "備註"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowNum = 1;
            for (PurchaseOrder order : orders) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(order.getPurchaseNo());
                row.createCell(1).setCellValue(order.getPurchaseDate() != null ? order.getPurchaseDate().toString() : "");
                row.createCell(2).setCellValue(order.getSupplier() != null ? order.getSupplier().getShortName() : "");
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