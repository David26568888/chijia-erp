package com.chijia.erp.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.chijia.erp.model.entity.SaleOrder;

@Repository
public interface SaleOrderRepository extends JpaRepository<SaleOrder, Long> {
	
	//檢查單號是否重覆
	boolean existsBySaleNo(String saleNo);
	
	Optional<SaleOrder> findBySaleNo(String saleNo);
}
