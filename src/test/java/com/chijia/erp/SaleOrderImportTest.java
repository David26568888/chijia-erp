package com.chijia.erp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.chijia.erp.model.dto.CreateSaleOrderDTO;
import com.chijia.erp.model.dto.SaleOrderDTO;
import com.chijia.erp.model.entity.Product;
import com.chijia.erp.repository.ProductRepository;
import com.chijia.erp.service.SaleOrderService;

@SpringBootTest
@Transactional
public class SaleOrderImportTest {

    @Autowired
    private SaleOrderService saleOrderService;

    @Autowired
    private ProductRepository productRepository;

    @Test
    public void testImportHistoricalSaleOrderWithoutDeductingStock() {
        // 1. 準備測試商品（假設原庫存為 100）
        Product product = new Product();
        product.setProductCode("TEST-001");
        product.setProductName("黃銅鎖頭");
        product.setStockQuantity(new BigDecimal("100"));
        product.setSalePrice(new BigDecimal("150"));
        Product savedProduct = productRepository.save(product);

        // 2. 建立歷史銷貨單 DTO，設定 deductStock = false (歷史匯入不扣庫存)
        CreateSaleOrderDTO createDTO = new CreateSaleOrderDTO();
        createDTO.setSaleDate(LocalDate.of(2026, 6, 1)); // 模擬過往日期
        createDTO.setDeductStock(false); // 💡 關鍵：不影響現有庫存

        CreateSaleOrderDTO.CreateItemDTO itemDTO = new CreateSaleOrderDTO.CreateItemDTO();
        itemDTO.setProductId(savedProduct.getId());
        itemDTO.setQuantity(new BigDecimal("10")); // 銷售 10 個
        itemDTO.setUnitPrice(new BigDecimal("150"));

        createDTO.setItems(List.of(itemDTO)); // 若你的 setter 名稱不同請對應調整

        // 3. 執行建立銷貨單
        SaleOrderDTO createdOrder = saleOrderService.createSaleOrder(createDTO);

        // 4. 驗證結果
        assertNotNull(createdOrder);
        assertEquals(LocalDate.of(2026, 6, 1), createdOrder.getSaleDate());

        // 5. 驗證商品庫存「維持原本的 100 不變」（因為是歷史匯入，不應被重複扣減）
        Product updatedProduct = productRepository.findById(savedProduct.getId()).orElseThrow();
        assertEquals(0, new BigDecimal("100").compareTo(updatedProduct.getStockQuantity()), 
            "歷史匯入不應影響現有庫存！庫存應保持 100");
    }
}