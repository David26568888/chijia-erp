package com.chijia.erp.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.chijia.erp.model.entity.Supplier;

@Repository
public interface SupplierRepository extends JpaRepository<Supplier, Long>{
	
	// 1. 透過「廠商編號」快速查詢
	Optional<Supplier> findBySupplierCode(String supplierCode);

	// 💡 2. 檢查「廠商編號」是否存在 (防止新建重複廠商編號)
	boolean existsBySupplierCode(String supplierCode);
}