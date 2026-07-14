package com.chijia.erp.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.chijia.erp.model.entity.Customer;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {
	// 實務需求：進銷存系統經常需要透過「客戶編號」快速撈出客戶資料
	Optional<Customer> findByCustomerCode(String customerCode);
}
