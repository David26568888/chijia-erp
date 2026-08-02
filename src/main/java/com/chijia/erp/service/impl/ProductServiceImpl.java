package com.chijia.erp.service.impl;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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
import com.chijia.erp.util.ExcelHelper;

@Service
public class ProductServiceImpl implements ProductService {


	@Autowired
	private ProductRepository productRepository;
	
	@Autowired
	private ProductMapper productMapper;

	@Override
	public List<ProductDTO> getAllProducts() {
		
		return productRepository.findAll().stream()
				.map(productMapper::toDTO)
				.collect(Collectors.toList());
	}

	@Override
	public ProductDTO getProductById(Long id) {
		Product product = productRepository.findById(id)
				.orElseThrow(()->new RuntimeException("找不到該商品，ID: " + id));
		
		return productMapper.toDTO(product);
	}

	@Override
	@Transactional
	public ProductDTO creatProduct(ProductDTO productDTO) {
		// 商業邏輯檢查：產品編號不能重複
		if(productRepository.findByProductCode(productDTO.getProductCode()).isPresent()) {
			throw new RuntimeException("產品編號 [" + productDTO.getProductCode() + "] 已存在，無法新增！");
		}
		
		Product product = productMapper.toEntity(productDTO);
		product.setStatus(true);
		
		Product savedProduct = productRepository.save(product);
		
		return productMapper.toDTO(savedProduct);
	}

	@Override
	@Transactional
	public ProductDTO updateProduct(Long id, ProductDTO productDTO) {
		Product existingProduct = productRepository.findById(id)
				.orElseThrow(()-> new RuntimeException("找不到該商品，無法更新！ID: " + id));
		
		// 覆蓋產品新欄位
		existingProduct.setProductName(productDTO.getProductName());
        existingProduct.setBarcode(productDTO.getBarcode());
        existingProduct.setUnit(productDTO.getUnit());
        existingProduct.setSalePrice(productDTO.getSalePrice());
        existingProduct.setStockQuantity(productDTO.getStockQuantity());
        
        Product updateProduct = productRepository.save(existingProduct);
        
		return productMapper.toDTO(updateProduct);
	}

	@Override
	@Transactional
	public void toggleStatus(Long id) {
		Product product = productRepository.findById(id)
				.orElseThrow(() -> new RuntimeException("找不到該商品，無法切換狀態！ID: " + id));
		
		product.setStatus(!product.isStatus());
		productRepository.save(product);
	}

	@Override
	public List<ProductDTO> searchProductByName(String name) {
		// 調用我們之前在 ProductRepository 定義的模糊查詢方法
		
		return productRepository.findByProductNameContaining(name).stream()
				.map(productMapper::toDTO)
				.collect(Collectors.toList());
	}

	@Override
	public ProductDTO getProductByBarcode(String barcode) {
	    // 呼叫你在 Repository 寫的 findByBarcode
	    Product product = productRepository.findByBarcode(barcode)
	            .orElseThrow(() -> new RuntimeException("找不到該條碼的商品，條碼: " + barcode));
	    return productMapper.toDTO(product); // 轉換成 DTO 回傳
	}

	@Override
	public ProductDTO getProductByProductCode(String productCode) {
	    // 呼叫你在 Repository 寫的 findByProductCode
	    Product product = productRepository.findByProductCode(productCode)
	            .orElseThrow(() -> new RuntimeException("找不到該產品編號的商品，編號: " + productCode));
	    return productMapper.toDTO(product); // 轉換成 DTO 回傳
}

	@Override
    @Transactional(rollbackFor = Exception.class)
    public String importProductsFromExcel(InputStream inputStream) throws Exception {
        Workbook workbook = WorkbookFactory.create(inputStream);
        
        // 讀取產品資料分頁 "bitem"
        Sheet sheet = workbook.getSheet("bitem");
        if (sheet == null) {
            sheet = workbook.getSheetAt(0);
        }

        List<Product> productList = new ArrayList<>();
        int importCount = 0;

        for (int i = 3; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) {
                continue;
            }

            // 讀取產品編號，若為空或星號 "*" 則跳過
            String productCode = ExcelHelper.getCellValueAsString(row.getCell(0));
            if (productCode.isEmpty() || "*".equals(productCode)) {
                continue;
            }

            Product product = new Product();
            product.setProductCode(productCode); // 產品編號 (第 0 欄)
            product.setProductName(ExcelHelper.getCellValueAsString(row.getCell(1))); // 品名規格 (第 1 欄)
            product.setBarcode(ExcelHelper.getCellValueAsString(row.getCell(2))); // 條碼編號 (第 2 欄)
            product.setUnit(ExcelHelper.getCellValueAsString(row.getCell(3))); // 單位 (第 3 欄)
            
            // 安全讀取數值
            double salePriceVal = ExcelHelper.getCellValueAsDouble(row.getCell(6), 0.0);
            double costPriceVal = ExcelHelper.getCellValueAsDouble(row.getCell(8), 0.0);
            
            // 💡 補上：讀取安全存量 (第 13 欄) 與 庫存總數量 (第 25 欄)
            int safetyStockVal = (int) ExcelHelper.getCellValueAsDouble(row.getCell(13), 0.0);
            int stockQtyVal = (int) ExcelHelper.getCellValueAsDouble(row.getCell(25), 0.0);
            
            product.setSalePrice(BigDecimal.valueOf(salePriceVal)); // 售價 (第 6 欄)
            product.setCostPrice(BigDecimal.valueOf(costPriceVal)); // 進價 (第 8 欄)
            
            //設定新產品的庫存與安全存量
            product.setSafetyStock(safetyStockVal);
            product.setStockQuantity(stockQtyVal);
            
            product.setStatus(true); // 預設啟用 (上架)

            // 防重複匯入：利用產品編號檢查
            Optional<Product> existingProductOpt = productRepository.findByProductCode(productCode);
            if (existingProductOpt.isPresent()) {
                Product existingProduct = existingProductOpt.get();
                existingProduct.setProductName(product.getProductName());
                existingProduct.setBarcode(product.getBarcode());
                existingProduct.setUnit(product.getUnit());
                
                existingProduct.setSalePrice(product.getSalePrice());
                existingProduct.setCostPrice(product.getCostPrice());
                
                // 💡 更新既有商品的庫存與安全存量
                existingProduct.setSafetyStock(product.getSafetyStock());
                existingProduct.setStockQuantity(product.getStockQuantity());
                
                productList.add(existingProduct);
            } else {
                productList.add(product);
            }

            importCount++;
        }

        if (!productList.isEmpty()) {
            productRepository.saveAll(productList);
        }

        workbook.close();
        return "成功匯入/更新 " + importCount + " 筆產品資料！";
    }
	}
