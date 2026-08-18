
package com.chijia.erp.config;

import com.chijia.erp.model.entity.Customer;
import com.chijia.erp.repository.CustomerRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final CustomerRepository customerRepository;

    public DataInitializer(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        // 💡 系統啟動時檢查：若無散客資料則自動播種 (Seed Data)
        if (!customerRepository.existsByCustomerCode("*")) {
            log.info("偵測到系統未設定散客主檔，自動建立預設散客 [*]...");
            Customer defaultRetailCustomer = new Customer();
            defaultRetailCustomer.setCustomerCode("*");
            defaultRetailCustomer.setShortName("散客");
            defaultRetailCustomer.setFullName("門市現金散客");
           
            customerRepository.save(defaultRetailCustomer);
            log.info("預設散客 [*] 建立完成！");
        }
    }
}