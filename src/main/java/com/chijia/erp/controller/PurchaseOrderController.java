package com.chijia.erp.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.chijia.erp.api.ApiResponse;
import com.chijia.erp.model.dto.CreatePurchaseOrderDTO;
import com.chijia.erp.model.dto.PurchaseOrderDTO;
import com.chijia.erp.service.PurchaseOrderService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/purchase-orders")
@CrossOrigin(origins = "*")
public class PurchaseOrderController {

    @Autowired
    private PurchaseOrderService purchaseOrderService;

    // 1. 新增進貨單 (建立單據並自動增加庫存)
    @PostMapping
    public ResponseEntity<ApiResponse<PurchaseOrderDTO>> createPurchaseOrder(@Valid @RequestBody CreatePurchaseOrderDTO createDTO) {
        PurchaseOrderDTO createdOrder = purchaseOrderService.createPurchaseOrder(createDTO);
        return new ResponseEntity<>(ApiResponse.success("進貨單建立成功，商品庫存已自動增加！", createdOrder), HttpStatus.CREATED);
    }

    // 2. 依 ID 查詢單一進貨單
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PurchaseOrderDTO>> getPurchaseOrderById(@PathVariable Long id) {
        PurchaseOrderDTO purchaseOrder = purchaseOrderService.getPurchaseOrderById(id);
        return ResponseEntity.ok(ApiResponse.success("進貨單查詢成功", purchaseOrder));
    }
}