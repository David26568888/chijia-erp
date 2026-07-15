package com.chijia.erp.service;

import java.io.InputStream;
import java.util.List;

import com.chijia.erp.model.dto.ProductDTO;


public interface ProductService {
	// 1. 查詢所有產品 
	List<ProductDTO> getAllProducts();
	
	//2.透過ID 查詢單一產品
	ProductDTO getProductById(Long id);
	
	//3.新增產品
	ProductDTO creatProduct(ProductDTO productDTO);
	
	//4.修改產品
	ProductDTO updateProduct(Long id, ProductDTO productDTO);
	
	//5.修改產品狀態
	void toggleStatus(Long id);
	
	//6.五金行必備：依據品名規格模糊搜尋產品
	List<ProductDTO> searchProductByName(String name);

	// 7. 依據條碼（Barcode）查詢單一產品 (櫃檯掃描槍用)
    ProductDTO getProductByBarcode(String barcode);
    
    // 8. 依據產品編號（ProductCode）查詢單一產品
    ProductDTO getProductByProductCode(String productCode);
    
   //9.解析並匯入商品 Excel 資料
    String importProductsFromExcel(InputStream inputStream) throws Exception;
}
