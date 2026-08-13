package com.chijia.erp.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.chijia.erp.model.entity.Product;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    // 1. 實務需求：櫃檯刷條碼時，用「條碼編號」撈出商品
    Optional<Product> findByBarcode(String barcode);

    // 2. 實務需求：後端用「產品編號」撈出商品
    Optional<Product> findByProductCode(String productCode);

    // 💡 3. 補強需求：檢查產品編號是否存在 (新增商品與 Excel 匯入防重使用)
    boolean existsByProductCode(String productCode);

    // 💡 4. 補強需求：商品名稱不分大小寫模糊查詢 (搜尋體驗更佳)
    List<Product> findByProductNameContainingIgnoreCase(String productName);
    
    //5.補強增強搜尋
    List<Product> findByProductNameContainingIgnoreCaseOrProductCodeContainingIgnoreCaseOrBarcodeContainingIgnoreCase(
    	    String name, String code, String barcode
    	);
}