package com.chijia.erp.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.chijia.erp.api.ApiResponse;
import com.chijia.erp.model.dto.CreateSaleOrderDTO;
import com.chijia.erp.model.dto.SaleOrderDTO;
import com.chijia.erp.service.SaleOrderService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/sale-orders")
public class SaleOrderController {

    @Autowired
    private SaleOrderService saleOrderService;

    // 1. 查詢所有銷貨單 (帶關鍵字與日期區間條件搜尋)
    @GetMapping
    public ResponseEntity<ApiResponse<List<SaleOrderDTO>>> getAllSaleOrders(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(value = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        List<SaleOrderDTO> list;
        if ((keyword != null && !keyword.trim().isEmpty()) || startDate != null || endDate != null) {
            list = saleOrderService.searchSaleOrders(keyword, startDate, endDate);
        } else {
            list = saleOrderService.getAllSaleOrders();
        }
        return ResponseEntity.ok(ApiResponse.success("銷貨單列表取得成功", list));
    }

    // 2. 查詢單一銷貨單[cite: 6]
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SaleOrderDTO>> getSaleOrderById(@PathVariable Long id) {
        SaleOrderDTO saleOrder = saleOrderService.getSaleOrderById(id);
        return ResponseEntity.ok(ApiResponse.success("銷貨單查詢成功", saleOrder)); //[cite: 6]
    }

    // 3. 新增銷貨單 (門市 POS 快速開單 + 自動扣庫存)[cite: 6]
    @PostMapping
    public ResponseEntity<ApiResponse<SaleOrderDTO>> createSaleOrder(@Valid @RequestBody CreateSaleOrderDTO createDTO) {
        SaleOrderDTO createdOrder = saleOrderService.createSaleOrder(createDTO);
        return new ResponseEntity<>(ApiResponse.success("銷貨單建立成功，商品庫存已同步扣減！", createdOrder), HttpStatus.CREATED); //[cite: 6]
    }

    // 4. 修改銷貨單 (自動差額多退少補)
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<SaleOrderDTO>> updateSaleOrder(
            @PathVariable Long id, 
            @Valid @RequestBody CreateSaleOrderDTO updateDTO) {
        SaleOrderDTO updatedOrder = saleOrderService.updateSaleOrder(id, updateDTO);
        return ResponseEntity.ok(ApiResponse.success("銷貨單修更成功，庫存已重新校正！", updatedOrder));
    }

    // 5. 作廢/刪除銷貨單 (自動庫存 100% 回補)
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> deleteSaleOrder(@PathVariable Long id) {
        saleOrderService.deleteSaleOrder(id);
        return ResponseEntity.ok(ApiResponse.success("🗑️ 銷貨單已成功作廢，商品庫存已自動回補！"));
    }
    

 // 6. 💡【取得歷史建議售價 API】(已包裝 ApiResponse 格式)
    @GetMapping("/suggest-price")
    public ResponseEntity<ApiResponse<BigDecimal>> getSuggestedPrice(
            @RequestParam Long customerId,
            @RequestParam Long productId) {
        BigDecimal price = saleOrderService.getSuggestedPrice(customerId, productId);
        return ResponseEntity.ok(ApiResponse.success("取得歷史建議售價成功", price));
    }
  
}