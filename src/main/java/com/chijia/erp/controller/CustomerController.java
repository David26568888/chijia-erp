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
import com.chijia.erp.model.dto.CustomerDTO;
import com.chijia.erp.service.CustomerService;

@RestController
@RequestMapping("/api/v1/customers")
@CrossOrigin(origins = "*")
public class CustomerController {

	@Autowired
	private CustomerService customerService;
	
	// 1. 查詢所有客戶：GET /api/v1/customers
	@GetMapping
	public ResponseEntity<ApiResponse<List<CustomerDTO>>> getAllCustomers(){
		List<CustomerDTO> customers = customerService.getAllCustomers();
		return ResponseEntity.ok(ApiResponse.success(customers));
	}
	
	// 2. 透過 ID 查詢單一客戶：GET /api/v1/customers/{id}
	@GetMapping("/{id}")
	public ResponseEntity<ApiResponse<CustomerDTO>> getCustomerById(@PathVariable Long id){
		CustomerDTO customer = customerService.getCustomerById(id);
		return ResponseEntity.ok(ApiResponse.success("客戶用id查詢成功",customer));
	}
	
	// 3. 新增客戶：POST /api/v1/customers
	@PostMapping
	public ResponseEntity<ApiResponse<CustomerDTO>> creatCustomer(@RequestBody CustomerDTO customerDTO){
		CustomerDTO createdCustomer = customerService.createCustomer(customerDTO);
		return new ResponseEntity<>(ApiResponse.success("客戶新增成功", createdCustomer),
				HttpStatus.CREATED);	// 回傳 201 Created
	}
	
	// 4. 修改客戶資料：PUT /api/v1/customers/{id}
	@PutMapping("/{id}")
	public ResponseEntity<ApiResponse<CustomerDTO>> updateCustomer(@PathVariable Long id, @RequestBody CustomerDTO customerDTO){
		CustomerDTO updatedCustomer = customerService.updateCustomer(id, customerDTO);
		return ResponseEntity.ok(ApiResponse.success("客戶修改成功", updatedCustomer));
	}
	
	// 5. 切換客戶啟用狀態：PATCH /api/v1/customers/{id}/toggle
	@PatchMapping("/{id}/toggle")
	public ResponseEntity<ApiResponse<Void>> toggleStatus(@PathVariable Long id){
		customerService.toggleStatus(id);
		return ResponseEntity.ok(ApiResponse.success("客戶狀態切換成功",null));// 回傳 204 No Content
	}
	
	// 6. 批次匯入客戶 Excel：POST /api/v1/customers/import
		@PostMapping("/import")
		public ResponseEntity<ApiResponse<String>> importCustomers(@RequestParam("file") MultipartFile file) {
			try {
				String result = customerService.importCustomersFromExcel(file.getInputStream());
				return ResponseEntity.ok(ApiResponse.success("客戶匯入成功", result));
			} catch (Exception e) {
				e.printStackTrace();
				return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
						.body(ApiResponse.error(500, "客戶匯入失敗: " + e.getMessage()));
			}
		}
}
