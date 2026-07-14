package com.chijia.erp.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.chijia.erp.model.entity.Product;
import java.util.List;


@Repository
public interface ProductRepository extends JpaRepository<Product, Long>{
	// 實務需求：櫃檯刷條碼時，用「條碼編號」撈出商品
	Optional<Product> findByBarcode(String barcode);
	
	// 實務需求：後端用「產品編號」撈出商品
	Optional<Product> findByProductCode(String productCode);
	
	// 實務需求：商品名稱模糊查詢（例如輸入"螺絲"，可以找出所有螺絲產品）
	List<Product> findByProductNameContaining(String productName);
	
}
