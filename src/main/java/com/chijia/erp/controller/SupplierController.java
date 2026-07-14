package com.chijia.erp.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.chijia.erp.model.dto.SupplierDTO;
import com.chijia.erp.service.SupplierService;

@RestController
@RequestMapping("/api/v1/suppliers")
@CrossOrigin(origins = "*")// 允許前端 React 跨域存取 (實務上 React 通常跑在 3000 埠)
public class SupplierController {
	
	@Autowired
	private SupplierService supplierService;
	
	// 1. 查詢所有廠商：GET /api/v1/suppliers
	@GetMapping
	public ResponseEntity<List<SupplierDTO>> getAllSuppliers(){
		List<SupplierDTO> suppliers = supplierService.getAllSuppliers();
		return ResponseEntity.ok(suppliers);
	}
	// 2. 透過 ID 查詢單一廠商：GET /api/v1/suppliers/{id}
	@GetMapping("/{id}")
	public ResponseEntity<SupplierDTO> getSupplierById(@PathVariable Long id){
		SupplierDTO supplier = supplierService.getSupplierById(id);
		return ResponseEntity.ok(supplier);
	}
	
	// 3. 新增廠商：POST /api/v1/suppliers
	@PostMapping
	public ResponseEntity<SupplierDTO> creatSupplier(@RequestBody SupplierDTO supplierDTO){
		SupplierDTO createdSupplier = supplierService.createSupplier(supplierDTO);
		// 回傳 201 Created 狀態碼
		return new ResponseEntity<>(createdSupplier,HttpStatus.CREATED);
	}
	
	// 4. 修改廠商：PUT /api/v1/suppliers/{id}
	@PutMapping("/{id}")
	public ResponseEntity<SupplierDTO> updatSupplier(@PathVariable Long id, @RequestBody SupplierDTO supplierDTO){
		SupplierDTO updateSupplier = supplierService.updateSupplier(id, supplierDTO);
		return ResponseEntity.ok(updateSupplier);
	}
	
	// 5. 切換啟用狀態：PATCH /api/v1/suppliers/{id}/toggle
	@PatchMapping("/{id}/toggle")
	public ResponseEntity<Void> toggleStatus(@PathVariable Long id){
		supplierService.toggleStatus(id);
		return ResponseEntity.noContent().build();
	}
}
