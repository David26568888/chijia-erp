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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.web.multipart.MultipartFile;

import com.chijia.erp.api.ApiResponse;
import com.chijia.erp.model.dto.ProductDTO;
import com.chijia.erp.service.ProductService;

@RestController
@RequestMapping("/api/v1/products")
@CrossOrigin(origins = "*")// 允許前端 React 跨域存取 (實務上 React 通常跑在 3000 埠)
public class ProductController {
	
	@Autowired
	private ProductService productService;
	
	// 1. 查詢所有產品：GET /api/v1/products
	@GetMapping
	public ResponseEntity<ApiResponse<List<ProductDTO>>> getAllProducts(){
		List<ProductDTO> products = productService.getAllProducts();
		return ResponseEntity.ok(ApiResponse.success(products));
	}
	
	// 2. 透過 ID 查詢單一產品：GET /api/v1/products/{id}
	@GetMapping("/{id}")
	public ResponseEntity<ApiResponse<ProductDTO>> getProductById(@PathVariable Long id){
		ProductDTO product = productService.getProductById(id);
		return ResponseEntity.ok(ApiResponse.success("商品用id查詢成功",product));
	}
	
	// 3. 新增產品：POST /api/v1/products
	@PostMapping
	public ResponseEntity<ApiResponse<ProductDTO>> createProduct(@RequestBody ProductDTO productDTO){
		ProductDTO createdProduct = productService.createProduct(productDTO);
		// 💡 HTTP 狀態碼回 201 Created，帶上專屬提示字眼
		return new ResponseEntity<>(ApiResponse.success("商品新增成功", createdProduct)
					,HttpStatus.CREATED
		);
	}
	
	// 4. 修改產品資料：PUT /api/v1/products/{id}
	@PutMapping("/{id}")
	public ResponseEntity<ApiResponse<ProductDTO>> updateProduct(@PathVariable Long id,@RequestBody ProductDTO productDTO){
		ProductDTO updatedProduct = productService.updateProduct(id, productDTO);
		return ResponseEntity.ok(ApiResponse.success("商品修改成功", updatedProduct));
				
	}
	
	// 5. 切換商品上架狀態：PATCH /api/v1/products/{id}/toggle
	@PatchMapping("/{id}/toggle")
	public ResponseEntity<ApiResponse<Void>> toggleStatus(@PathVariable Long id){
		productService.toggleStatus(id);
		
		// 💡 實務上也可以選擇回傳 200 並告知操作成功，比 204 無內容更具備前端彈窗友善度！
		
		return ResponseEntity.ok(ApiResponse.success("上架狀態切換成功",null));
	}
	
	// 6. 依品名模糊搜尋：GET /api/v1/products/search?name=螺絲
	@GetMapping("/search")
	public ResponseEntity<ApiResponse<List<ProductDTO>>> getProductByName(@RequestParam String name){
		List<ProductDTO> products = productService.searchProductByName(name);
		return ResponseEntity.ok(ApiResponse.success("商品用名稱查詢成功", products));
				
	}
	
	// 7. 五金行掃描槍必備：依據條碼（Barcode）查詢單一產品
	@GetMapping("/barcode/{barcode}")
	public ResponseEntity<ApiResponse<ProductDTO>> getProductByBarcode(@PathVariable String barcode){ // 💡 改為 @PathVariable
	    ProductDTO product = productService.getProductByBarcode(barcode);
	    return ResponseEntity.ok(ApiResponse.success("商品用Barcode查詢成功",product));
	}

	// 8. 依據產品編號（ProductCode）查詢單一產品
	@GetMapping("/product-code/{productCode}")
	public ResponseEntity<ApiResponse<ProductDTO>> getProductByProductCode(@PathVariable String productCode){ // 💡 改為 @PathVariable
	    ProductDTO product = productService.getProductByProductCode(productCode);
	    return ResponseEntity.ok(ApiResponse.success("商品用商品編號查詢成功",product));
	}
	
	// 9. 批次匯入產品 Excel：POST /api/v1/products/import
		@PostMapping("/import")
		public ResponseEntity<ApiResponse<String>> importProducts(@RequestParam("file") MultipartFile file) {
			try {
				String result = productService.importProductsFromExcel(file.getInputStream());
				return ResponseEntity.ok(ApiResponse.success("產品匯入成功", result));
			} catch (Exception e) {
				e.printStackTrace();
				return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
						.body(ApiResponse.error(500, "產品匯入失敗: " + e.getMessage()));
			}
		}
	
}
