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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chijia.erp.mapper.ProductMapper;
import com.chijia.erp.model.dto.ProductDTO;
import com.chijia.erp.model.entity.Product;
import com.chijia.erp.repository.ProductRepository;
import com.chijia.erp.service.ProductService;
import com.chijia.erp.util.ExcelHelper; // 💡 引入統一的 Excel 工具類

@Service
public class ProductServiceImpl implements ProductService {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductMapper productMapper; // 手動 Mapper

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
    public List<ProductDTO> searchProductByName(String name) {
        return productRepository.findByProductNameContainingIgnoreCase(name).stream()
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

    // 9. 解析並匯入商品 Excel 資料 (呼叫通用 ExcelHelper 工具類)
    @Override
    @Transactional
    public String importProductsFromExcel(InputStream inputStream) throws Exception {
        int successCount = 0;
        int skipCount = 0;

        // 💡 高效能比對：將 DB 既有產品編號載入記憶體 Set 中
        Set<String> existingCodes = productRepository.findAll().stream()
                .map(Product::getProductCode)
                .collect(Collectors.toSet());

        try (Workbook workbook = WorkbookFactory.create(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0); // 讀取第一個工作表

            // 💡 表頭偏移處理：前 3 列為抬頭/日期資訊，第 4 列(i=3)為標題，第一筆真實商品從第 5 列(i=4)開始
            for (int i = 4; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                String productCode = ExcelHelper.getCellValueAsString(row.getCell(0)); // 索引 0: 產品編號
                String productName = ExcelHelper.getCellValueAsString(row.getCell(1)); // 索引 1: 品名規格

                if (productCode.isEmpty() || productName.isEmpty()) {
                    continue; // 必填欄位空白則跳過
                }

                // 若產品編號已存在則跳過，避免重複匯入
                if (existingCodes.contains(productCode)) {
                    skipCount++;
                    continue;
                }

                Product product = new Product();
                product.setProductCode(productCode);
                product.setProductName(productName);
                product.setBarcode(ExcelHelper.getCellValueAsString(row.getCell(2))); // 索引 2: 條碼
                product.setUnit(ExcelHelper.getCellValueAsString(row.getCell(3)));    // 索引 3: 單位

                // 💡 讀取價格、三軌成本與庫存 (使用高精度 ExcelHelper)
                BigDecimal salePrice     = ExcelHelper.getCellValueAsBigDecimal(row.getCell(6), BigDecimal.ZERO);  // 索引 6: 售價
                BigDecimal lastCostPrice = ExcelHelper.getCellValueAsBigDecimal(row.getCell(8), BigDecimal.ZERO);  // 索引 8: 進價 (最後進價)
                BigDecimal costPrice     = ExcelHelper.getCellValueAsBigDecimal(row.getCell(10), BigDecimal.ZERO); // 索引 10: 期初單位成本 (基準成本)
                BigDecimal avgCostPrice  = ExcelHelper.getCellValueAsBigDecimal(row.getCell(65), BigDecimal.ZERO); // 索引 65: 移動加權平均成本

                BigDecimal stockQty      = ExcelHelper.getCellValueAsBigDecimal(row.getCell(25), BigDecimal.ZERO); // 索引 25: 庫存總數量
                BigDecimal safetyStock   = ExcelHelper.getCellValueAsBigDecimal(row.getCell(13), BigDecimal.ZERO); // 索引 13: 安全存量

                product.setSalePrice(salePrice);
                product.setCostPrice(costPrice.compareTo(BigDecimal.ZERO) > 0 ? costPrice : lastCostPrice);
                product.setLastCostPrice(lastCostPrice);
                product.setAvgCostPrice(avgCostPrice.compareTo(BigDecimal.ZERO) > 0 ? avgCostPrice : lastCostPrice);

                product.setStockQuantity(stockQty);
                product.setSafetyStock(safetyStock);
                product.setStatus(true);

                productRepository.save(product);
                existingCodes.add(productCode); // 同步更新記憶體快取
                successCount++;
            }
        }

        return String.format("Excel 匯入完成！成功匯入 %d 筆，跳過（重複或無效） %d 筆。", successCount, skipCount);
    }
}