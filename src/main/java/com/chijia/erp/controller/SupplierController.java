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
import org.springframework.web.bind.annotation.RequestParam;

import org.springframework.web.multipart.MultipartFile;

import com.chijia.erp.api.ApiResponse;
import com.chijia.erp.model.dto.SupplierDTO;
import com.chijia.erp.service.SupplierService;

@RestController
@RequestMapping("/api/v1/suppliers")
public class SupplierController {
	
	@Autowired
	private SupplierService supplierService;
	
	// 1. 查詢所有廠商：GET /api/v1/suppliers
	@GetMapping
	public ResponseEntity<ApiResponse<List<SupplierDTO>>> getAllSuppliers(){
		List<SupplierDTO> suppliers = supplierService.getAllSuppliers();
		return ResponseEntity.ok(ApiResponse.success(suppliers));
	}
	// 2. 透過 ID 查詢單一廠商：GET /api/v1/suppliers/{id}
	@GetMapping("/{id}")
	public ResponseEntity<ApiResponse<SupplierDTO>> getSupplierById(@PathVariable Long id){
		SupplierDTO supplier = supplierService.getSupplierById(id);
		return ResponseEntity.ok(ApiResponse.success("廠商用id查詢成功", supplier));
	}
	
	// 3. 新增廠商：POST /api/v1/suppliers
	@PostMapping
	public ResponseEntity<ApiResponse<SupplierDTO>> creatSupplier(@RequestBody SupplierDTO supplierDTO){
		SupplierDTO createdSupplier = supplierService.createSupplier(supplierDTO);
		// 回傳 201 Created 狀態碼
		return new ResponseEntity<>(ApiResponse.success("廠商新增成功", createdSupplier),HttpStatus.CREATED);
	}
	
	// 4. 修改廠商：PUT /api/v1/suppliers/{id}
	@PutMapping("/{id}")
	public ResponseEntity<ApiResponse<SupplierDTO>> updatSupplier(@PathVariable Long id, @RequestBody SupplierDTO supplierDTO){
		SupplierDTO updateSupplier = supplierService.updateSupplier(id, supplierDTO);
		return ResponseEntity.ok(ApiResponse.success("廠商修改成功", updateSupplier));
	}
	
	// 5. 切換啟用狀態：PATCH /api/v1/suppliers/{id}/toggle
	@PatchMapping("/{id}/toggle")
	public ResponseEntity<ApiResponse<Void>> toggleStatus(@PathVariable Long id){
		supplierService.toggleStatus(id);
		return ResponseEntity.ok(ApiResponse.success("廠商狀態切換成功", null));
	}
	
	// 6.批次匯入廠商 Excel：POST /api/v1/suppliers/import
		@PostMapping("/import")
		public ResponseEntity<ApiResponse<String>> importSuppliers(@RequestParam("file") MultipartFile file) {
			try {
				// 呼叫 Service 的匯入方法
				String result = supplierService.importSuppliersFromExcel(file.getInputStream());
				return ResponseEntity.ok(ApiResponse.success("匯入成功", result));
			} catch (Exception e) {
				e.printStackTrace(); // 在主控台印出詳細錯誤以便排查
				return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
						.body(ApiResponse.error(500, "匯入失敗: " + e.getMessage()));
			}
		}
}
