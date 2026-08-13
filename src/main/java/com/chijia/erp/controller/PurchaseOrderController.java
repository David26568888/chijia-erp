package com.chijia.erp.controller;

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
import com.chijia.erp.model.dto.CreatePurchaseOrderDTO;
import com.chijia.erp.model.dto.PurchaseOrderDTO;
import com.chijia.erp.service.PurchaseOrderService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/purchase-orders")
@CrossOrigin(origins = "*") //[cite: 14]
public class PurchaseOrderController {

    @Autowired
    private PurchaseOrderService purchaseOrderService;

    // 💡 1. 查詢所有進貨單 (帶關鍵字與日期條件搜尋)
    @GetMapping
    public ResponseEntity<ApiResponse<List<PurchaseOrderDTO>>> getAllPurchaseOrders(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(value = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        List<PurchaseOrderDTO> list;
        if ((keyword != null && !keyword.trim().isEmpty()) || startDate != null || endDate != null) {
            list = purchaseOrderService.searchPurchaseOrders(keyword, startDate, endDate);
        } else {
            list = purchaseOrderService.getAllPurchaseOrders();
        }
        return ResponseEntity.ok(ApiResponse.success("進貨單列表取得成功", list));
    }

    // 2. 依 ID 查詢單一進貨單[cite: 14]
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PurchaseOrderDTO>> getPurchaseOrderById(@PathVariable Long id) {
        PurchaseOrderDTO purchaseOrder = purchaseOrderService.getPurchaseOrderById(id);
        return ResponseEntity.ok(ApiResponse.success("進貨單查詢成功", purchaseOrder)); //[cite: 14]
    }

    // 3. 新增進貨單 (自動增加庫存 + 更新最後進價與加權平均成本)[cite: 14]
    @PostMapping
    public ResponseEntity<ApiResponse<PurchaseOrderDTO>> createPurchaseOrder(@Valid @RequestBody CreatePurchaseOrderDTO createDTO) {
        PurchaseOrderDTO createdOrder = purchaseOrderService.createPurchaseOrder(createDTO);
        return new ResponseEntity<>(ApiResponse.success("進貨單建立成功，商品庫存與三軌成本已自動更新！", createdOrder), HttpStatus.CREATED); //[cite: 14]
    }

    // 💡 4. 修改進貨單
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<PurchaseOrderDTO>> updatePurchaseOrder(
            @PathVariable Long id, 
            @Valid @RequestBody CreatePurchaseOrderDTO updateDTO) {
        PurchaseOrderDTO updatedOrder = purchaseOrderService.updatePurchaseOrder(id, updateDTO);
        return ResponseEntity.ok(ApiResponse.success("進貨單更新成功，庫存已重新校正！", updatedOrder));
    }

    // 💡 5. 作廢/刪除進貨單
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> deletePurchaseOrder(@PathVariable Long id) {
        purchaseOrderService.deletePurchaseOrder(id);
        return ResponseEntity.ok(ApiResponse.success("🗑️ 進貨單已成功作廢，庫存已自動扣回！"));
    }
}