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
import com.chijia.erp.model.dto.CreateSaleOrderDTO;
import com.chijia.erp.model.dto.SaleOrderDTO;
import com.chijia.erp.service.SaleOrderService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/sale-orders")
@CrossOrigin(origins = "*")
public class SaleOrderController {
	
	@Autowired
	private SaleOrderService saleOrderService;
	
	// 1. 新增銷貨單 (建立訂單並扣庫存)
	@PostMapping
	public ResponseEntity<ApiResponse<SaleOrderDTO>> createSaleOrder(@Valid@RequestBody CreateSaleOrderDTO createDTO){
		SaleOrderDTO createdOrder = saleOrderService.createSaleOrder(createDTO);
		return new ResponseEntity<>(ApiResponse.success("銷貨單建立成功，庫存已同步扣減！", createdOrder), HttpStatus.CREATED);
	}
	
	// 2. 查詢單一銷貨單
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SaleOrderDTO>> getSaleOrderById(@PathVariable Long id) {
        SaleOrderDTO saleOrder = saleOrderService.getSaleOrderById(id);
        return ResponseEntity.ok(ApiResponse.success("銷貨单查詢成功", saleOrder));
    }
}
