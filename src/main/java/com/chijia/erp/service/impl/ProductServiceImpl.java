package com.chijia.erp.service.impl;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chijia.erp.mapper.ProductMapper;
import com.chijia.erp.model.dto.ProductDTO;
import com.chijia.erp.model.dto.ProductHistoryDTO;
import com.chijia.erp.model.entity.Customer;
import com.chijia.erp.model.entity.Product;
import com.chijia.erp.model.entity.Supplier;
import com.chijia.erp.repository.ProductRepository;
import com.chijia.erp.repository.PurchaseOrderItemRepository;
import com.chijia.erp.repository.SaleOrderItemRepository;
import com.chijia.erp.service.ProductService;
import com.chijia.erp.util.ExcelHelper;

@Service
public class ProductServiceImpl implements ProductService {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private PurchaseOrderItemRepository purchaseOrderItemRepository;

    @Autowired
    private SaleOrderItemRepository saleOrderItemRepository;
    
    @Autowired
    private ProductMapper productMapper;

    // 1. 查詢所有產品
    @Override
    public List<ProductDTO> getAllProducts() {
        return productRepository.findAll().stream()
                .map(productMapper::toDTO)
                .collect(Collectors.toList());
    }

    // 2. 透過 ID 查詢單一產品
    @Override
    public ProductDTO getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("找不到產品，ID: " + id));
        return productMapper.toDTO(product);
    }

    // 3. 新增產品 (包含三軌成本防呆自動預設)
    @Override
    @Transactional
    public ProductDTO createProduct(ProductDTO productDTO) {
        if (productDTO.getProductCode() != null && productRepository.existsByProductCode(productDTO.getProductCode())) {
            throw new RuntimeException("產品編號已存在: " + productDTO.getProductCode());
        }

        Product product = productMapper.toEntity(productDTO);

        // 💡 三軌成本防呆：建檔時若只填預設成本 (costPrice)，自動為最後進價與平均成本填補初始預設值
        if (product.getCostPrice() != null) {
            if (product.getLastCostPrice() == null) {
                product.setLastCostPrice(product.getCostPrice());
            }
            if (product.getAvgCostPrice() == null) {
                product.setAvgCostPrice(product.getCostPrice());
            }
        }

        Product savedProduct = productRepository.save(product);
        return productMapper.toDTO(savedProduct);
    }

    // 4. 修改產品 (完整同步三軌成本)
    @Override
    @Transactional
    public ProductDTO updateProduct(Long id, ProductDTO productDTO) {
        Product existingProduct = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("找不到該產品，無法更新！ID: " + id));
        existingProduct.setProductCode(productDTO.getProductCode());
        existingProduct.setProductName(productDTO.getProductName());
        existingProduct.setBarcode(productDTO.getBarcode());
        existingProduct.setUnit(productDTO.getUnit());
        existingProduct.setSalePrice(productDTO.getSalePrice());

        // 💡 更新三軌成本
        existingProduct.setCostPrice(productDTO.getCostPrice());
        existingProduct.setLastCostPrice(productDTO.getLastCostPrice());
        existingProduct.setAvgCostPrice(productDTO.getAvgCostPrice());

        if (productDTO.getStockQuantity() != null) {
            existingProduct.setStockQuantity(productDTO.getStockQuantity());
        }
        if (productDTO.getSafetyStock() != null) {
            existingProduct.setSafetyStock(productDTO.getSafetyStock());
        }
        existingProduct.setStatus(productDTO.isStatus());

        Product updatedProduct = productRepository.save(existingProduct);
        return productMapper.toDTO(updatedProduct);
    }

    // 5. 修改產品啟用/停用狀態
    @Override
    @Transactional
    public void toggleStatus(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("找不到產品，ID: " + id));
        product.setStatus(!product.isStatus());
        productRepository.save(product);
    }

    // 6. 五金行必備：依據品名規格模糊搜尋產品
    @Override
    public List<ProductDTO> searchProductByName(String keyword) {
        return productRepository.findByProductNameContainingIgnoreCaseOrProductCodeContainingIgnoreCaseOrBarcodeContainingIgnoreCase(
                keyword, keyword, keyword
        ).stream()
        .map(productMapper::toDTO)
        .collect(Collectors.toList());
    }

    // 7. 依據條碼（Barcode）查詢單一產品 (櫃檯掃描槍用)
    @Override
    public ProductDTO getProductByBarcode(String barcode) {
        Product product = productRepository.findByBarcode(barcode)
                .orElseThrow(() -> new RuntimeException("找不到對應條碼的商品: " + barcode));
        return productMapper.toDTO(product);
    }

    // 8. 依據產品編號（ProductCode）查詢單一產品
    @Override
    public ProductDTO getProductByProductCode(String productCode) {
        Product product = productRepository.findByProductCode(productCode)
                .orElseThrow(() -> new RuntimeException("找不到對應產品編號的商品: " + productCode));
        return productMapper.toDTO(product);
    }

    // 9. 解析並匯入商品 Excel 資料
    @Override
    @Transactional
    public String importProductsFromExcel(InputStream inputStream) throws Exception {
        int successCount = 0;
        int skipCount = 0;

        Set<String> existingCodes = productRepository.findAll().stream()
                .map(Product::getProductCode)
                .collect(Collectors.toSet());

        try (Workbook workbook = WorkbookFactory.create(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);

            for (int i = 4; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                String productCode = ExcelHelper.getCellValueAsString(row.getCell(0));
                String productName = ExcelHelper.getCellValueAsString(row.getCell(1));

                if (productCode.isEmpty() || productName.isEmpty()) {
                    continue;
                }

                if (existingCodes.contains(productCode)) {
                    skipCount++;
                    continue;
                }

                Product product = new Product();
                product.setProductCode(productCode);
                product.setProductName(productName);
                product.setBarcode(ExcelHelper.getCellValueAsString(row.getCell(2)));
                product.setUnit(ExcelHelper.getCellValueAsString(row.getCell(3)));

                BigDecimal salePrice     = ExcelHelper.getCellValueAsBigDecimal(row.getCell(6), BigDecimal.ZERO);
                BigDecimal lastCostPrice = ExcelHelper.getCellValueAsBigDecimal(row.getCell(8), BigDecimal.ZERO);
                BigDecimal costPrice     = ExcelHelper.getCellValueAsBigDecimal(row.getCell(10), BigDecimal.ZERO);
                BigDecimal avgCostPrice  = ExcelHelper.getCellValueAsBigDecimal(row.getCell(65), BigDecimal.ZERO);

                BigDecimal stockQty      = ExcelHelper.getCellValueAsBigDecimal(row.getCell(25), BigDecimal.ZERO);
                BigDecimal safetyStock   = ExcelHelper.getCellValueAsBigDecimal(row.getCell(13), BigDecimal.ZERO);

                product.setSalePrice(salePrice);
                product.setCostPrice(costPrice.compareTo(BigDecimal.ZERO) > 0 ? costPrice : lastCostPrice);
                product.setLastCostPrice(lastCostPrice);
                product.setAvgCostPrice(avgCostPrice.compareTo(BigDecimal.ZERO) > 0 ? avgCostPrice : lastCostPrice);

                product.setStockQuantity(stockQty);
                product.setSafetyStock(safetyStock);
                product.setStatus(true);

                productRepository.save(product);
                existingCodes.add(productCode);
                successCount++;
            }
        }

        return String.format("Excel 匯入完成！成功匯入 %d 筆，跳過（重複或無效） %d 筆。", successCount, skipCount);
    }
    
    // 10. 系統備份還原
    @Override
    @Transactional
    public String restoreProductsFromBackup(InputStream inputStream) throws Exception {
        int successCount = 0;
        int skipCount = 0;

        Set<String> existingCodes = productRepository.findAll().stream()
                .map(Product::getProductCode)
                .collect(Collectors.toSet());

        try (Workbook workbook = WorkbookFactory.create(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                String productCode = ExcelHelper.getCellValueAsString(row.getCell(0));
                String productName = ExcelHelper.getCellValueAsString(row.getCell(2));

                if (productCode.isEmpty() || productName.isEmpty()) {
                    continue;
                }

                if (existingCodes.contains(productCode)) {
                    skipCount++;
                    continue;
                }

                Product product = new Product();
                product.setProductCode(productCode);
                product.setBarcode(ExcelHelper.getCellValueAsString(row.getCell(1)));
                product.setProductName(productName);
                product.setUnit(ExcelHelper.getCellValueAsString(row.getCell(3)));
                
                product.setSalePrice(ExcelHelper.getCellValueAsBigDecimal(row.getCell(4), BigDecimal.ZERO));
                product.setCostPrice(ExcelHelper.getCellValueAsBigDecimal(row.getCell(5), BigDecimal.ZERO));
                product.setLastCostPrice(ExcelHelper.getCellValueAsBigDecimal(row.getCell(6), BigDecimal.ZERO));
                product.setAvgCostPrice(ExcelHelper.getCellValueAsBigDecimal(row.getCell(7), BigDecimal.ZERO));
                product.setStockQuantity(ExcelHelper.getCellValueAsBigDecimal(row.getCell(8), BigDecimal.ZERO));
                product.setSafetyStock(ExcelHelper.getCellValueAsBigDecimal(row.getCell(9), BigDecimal.ZERO));
                
                String statusStr = ExcelHelper.getCellValueAsString(row.getCell(10));
                product.setStatus("上架".equals(statusStr) || statusStr.isEmpty());

                productRepository.save(product);
                existingCodes.add(productCode);
                successCount++;
            }
        }

        return String.format("系統備份還原完成！成功匯入 %d 筆，跳過重複 %d 筆。", successCount, skipCount);
    }

 // 11. 查看商品相關進貨紀錄 / 銷貨紀錄
    @Override
    public ProductHistoryDTO getProductHistory(Long productId) {
        // 取最新的前 20 筆歷史紀錄
        PageRequest pageRequest = PageRequest.of(0, 20);

        // 💡 1. 撈取進貨歷史 (傳入變數 productId 與 pageRequest，非型態名稱)
        List<ProductHistoryDTO.PurchaseRecordDTO> purchaseHistory = purchaseOrderItemRepository
                .findRecentHistoryByProductId(productId, pageRequest)
                .stream()
                .map(item -> {
                    String supplierName = "門市進貨";
                    if (item.getPurchaseOrder() != null && item.getPurchaseOrder().getSupplier() != null) {
                        Supplier supplier = item.getPurchaseOrder().getSupplier();
                        supplierName = supplier.getShortName() != null && !supplier.getShortName().isBlank()
                                ? supplier.getShortName() 
                                : supplier.getFullName();
                    }
                    return new ProductHistoryDTO.PurchaseRecordDTO(
                            supplierName,
                            item.getPurchaseOrder() != null ? item.getPurchaseOrder().getPurchaseDate() : null,
                            item.getUnitPrice(),
                            item.getQuantity()
                    );
                })
                .collect(Collectors.toList());

        // 💡 2. 撈取銷貨歷史
        List<ProductHistoryDTO.SaleRecordDTO> saleHistory = saleOrderItemRepository
                .findRecentHistoryByProductId(productId, pageRequest)
                .stream()
                .map(item -> {
                    String customerName = "門市散客";
                    if (item.getSaleOrder() != null && item.getSaleOrder().getCustomer() != null) {
                        Customer customer = item.getSaleOrder().getCustomer();
                        customerName = customer.getShortName() != null && !customer.getShortName().isBlank()
                                ? customer.getShortName() 
                                : customer.getFullName();
                    }
                    return new ProductHistoryDTO.SaleRecordDTO(
                            customerName,
                            item.getSaleOrder() != null ? item.getSaleOrder().getSaleDate() : null,
                            item.getUnitPrice(),
                            item.getQuantity()
                    );
                })
                .collect(Collectors.toList());

        return new ProductHistoryDTO(purchaseHistory, saleHistory);
    }
}