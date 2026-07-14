package com.chijia.erp.service.impl;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.chijia.erp.mapper.ProductMapper;
import com.chijia.erp.model.dto.ProductDTO;
import com.chijia.erp.model.entity.Product;
import com.chijia.erp.repository.ProductRepository;
import com.chijia.erp.service.ProductService;

@Service
public class ProductServiceImpl implements ProductService {


	@Autowired
	private ProductRepository productRepository;
	
	@Autowired
	private ProductMapper productMapper;

	@Override
	public List<ProductDTO> getAllProducts() {
		
		return productRepository.findAll().stream()
				.map(productMapper::toDTO)
				.collect(Collectors.toList());
	}

	@Override
	public ProductDTO getProductById(Long id) {
		Product product = productRepository.findById(id)
				.orElseThrow(()->new RuntimeException("找不到該商品，ID: " + id));
		
		return productMapper.toDTO(product);
	}

	@Override
	@Transactional
	public ProductDTO creatProduct(ProductDTO productDTO) {
		// 商業邏輯檢查：產品編號不能重複
		if(productRepository.findByProductCode(productDTO.getProductCode()).isPresent()) {
			throw new RuntimeException("產品編號 [" + productDTO.getProductCode() + "] 已存在，無法新增！");
		}
		
		Product product = productMapper.toEntity(productDTO);
		product.setStatus(true);
		
		Product savedProduct = productRepository.save(product);
		
		return productMapper.toDTO(savedProduct);
	}

	@Override
	@Transactional
	public ProductDTO updateProduct(Long id, ProductDTO productDTO) {
		Product existingProduct = productRepository.findById(id)
				.orElseThrow(()-> new RuntimeException("找不到該商品，無法更新！ID: " + id));
		
		// 覆蓋產品新欄位
		existingProduct.setProductName(productDTO.getProductName());
        existingProduct.setBarcode(productDTO.getBarcode());
        existingProduct.setUnit(productDTO.getUnit());
        existingProduct.setSalePrice(productDTO.getSalePrice());
        existingProduct.setStockQuantity(productDTO.getStockQuantity());
        
        Product updateProduct = productRepository.save(existingProduct);
        
		return productMapper.toDTO(updateProduct);
	}

	@Override
	@Transactional
	public void toggleStatus(Long id) {
		Product product = productRepository.findById(id)
				.orElseThrow(() -> new RuntimeException("找不到該商品，無法切換狀態！ID: " + id));
		
		product.setStatus(!product.isStatus());
		productRepository.save(product);
	}

	@Override
	public List<ProductDTO> searchProductByName(String name) {
		// 調用我們之前在 ProductRepository 定義的模糊查詢方法
		
		return productRepository.findByProductNameContaining(name).stream()
				.map(productMapper::toDTO)
				.collect(Collectors.toList());
	}

	@Override
	public ProductDTO getProductByBarcode(String barcode) {
	    // 呼叫你在 Repository 寫的 findByBarcode
	    Product product = productRepository.findByBarcode(barcode)
	            .orElseThrow(() -> new RuntimeException("找不到該條碼的商品，條碼: " + barcode));
	    return productMapper.toDTO(product); // 轉換成 DTO 回傳
	}

	@Override
	public ProductDTO getProductByProductCode(String productCode) {
	    // 呼叫你在 Repository 寫的 findByProductCode
	    Product product = productRepository.findByProductCode(productCode)
	            .orElseThrow(() -> new RuntimeException("找不到該產品編號的商品，編號: " + productCode));
	    return productMapper.toDTO(product); // 轉換成 DTO 回傳
}
	}
