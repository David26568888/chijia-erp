package com.chijia.erp.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.chijia.erp.model.entity.Supplier;

@Repository
public interface SupplierRepository extends JpaRepository<Supplier, Long>{
	// 實務需求：透過「廠商編號」快速查詢
	Optional<Supplier> findBySupplierCode(String supplierCode);

}
