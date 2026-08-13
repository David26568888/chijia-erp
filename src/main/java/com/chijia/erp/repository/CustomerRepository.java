package com.chijia.erp.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.chijia.erp.model.entity.Customer;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    // 1. 透過「客戶編號」查詢單一客戶
    Optional<Customer> findByCustomerCode(String customerCode);

    // 💡 2. 檢查「客戶編號」是否存在 (避免重複建檔)
    boolean existsByCustomerCode(String customerCode);

    // 💡 3. 模糊搜尋：依客戶簡稱、客戶編號或電話進行搜尋
    List<Customer> findByShortNameContainingIgnoreCaseOrCustomerCodeContainingIgnoreCaseOrPhoneContainingIgnoreCase(
            String shortName, String customerCode, String phone);
}