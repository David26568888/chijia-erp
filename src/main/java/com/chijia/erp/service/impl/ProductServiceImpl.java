package com.chijia.erp.service.impl;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
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

    // 6. 五金行必備：依據品名規格模糊搜尋產品 (與介面 searchProductByName 完全對齊)
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

    // 9. 解析並匯入商品 Excel 資料 (使用 Apache POI)
    @Override
    @Transactional
    public String importProductsFromExcel(InputStream inputStream) throws Exception {
        int successCount = 0;
        int skipCount = 0;

        try (Workbook workbook = WorkbookFactory.create(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0); // 讀取第一個工作表

            for (int i = 1; i <= sheet.getLastRowNum(); i++) { // 從第 2 列開始 (第 1 列為標題)
                Row row = sheet.getRow(i);
                if (row == null) continue;

                String productCode = getCellValueAsString(row.getCell(0));
                String productName = getCellValueAsString(row.getCell(1));

                if (productCode == null || productCode.trim().isEmpty() || productName == null || productName.trim().isEmpty()) {
                    continue; // 必填欄位空白則跳過
                }

                // 若產品編號已存在則跳過，避免重複匯入
                if (productRepository.existsByProductCode(productCode)) {
                    skipCount++;
                    continue;
                }

                Product product = new Product();
                product.setProductCode(productCode);
                product.setProductName(productName);
                product.setBarcode(getCellValueAsString(row.getCell(2)));
                product.setUnit(getCellValueAsString(row.getCell(3)));
                
                // 讀取價格與庫存
                BigDecimal costPrice = getCellValueAsBigDecimal(row.getCell(4));
                BigDecimal salePrice = getCellValueAsBigDecimal(row.getCell(5));
                BigDecimal stockQty = getCellValueAsBigDecimal(row.getCell(6));
                BigDecimal safetyStock = getCellValueAsBigDecimal(row.getCell(7));

                product.setCostPrice(costPrice != null ? costPrice : BigDecimal.ZERO);
                product.setLastCostPrice(costPrice != null ? costPrice : BigDecimal.ZERO);
                product.setAvgCostPrice(costPrice != null ? costPrice : BigDecimal.ZERO);
                
                product.setSalePrice(salePrice != null ? salePrice : BigDecimal.ZERO);
                product.setStockQuantity(stockQty != null ? stockQty : BigDecimal.ZERO);
                product.setSafetyStock(safetyStock != null ? safetyStock : BigDecimal.ZERO);
                product.setStatus(true);

                productRepository.save(product);
                successCount++;
            }
        }

        return String.format("Excel 匯入完成！成功匯入 %d 筆，跳過（重複或無效） %d 筆。", successCount, skipCount);
    }

    // 🛠️ Excel 儲存格字串輔助轉譯工具
    private String getCellValueAsString(Cell cell) {
        if (cell == null) return "";
        if (cell.getCellType() == CellType.STRING) {
            return cell.getStringCellValue().trim();
        } else if (cell.getCellType() == CellType.NUMERIC) {
            return String.valueOf((long) cell.getNumericCellValue());
        }
        return "";
    }

    // 🛠️ Excel 儲存格數值輔助轉譯工具
    private BigDecimal getCellValueAsBigDecimal(Cell cell) {
        if (cell == null) return BigDecimal.ZERO;
        if (cell.getCellType() == CellType.NUMERIC) {
            return BigDecimal.valueOf(cell.getNumericCellValue());
        } else if (cell.getCellType() == CellType.STRING) {
            try {
                return new BigDecimal(cell.getStringCellValue().trim());
            } catch (Exception e) {
                return BigDecimal.ZERO;
            }
        }
        return BigDecimal.ZERO;
    }
}