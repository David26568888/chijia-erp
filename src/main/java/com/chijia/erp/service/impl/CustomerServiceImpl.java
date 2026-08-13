package com.chijia.erp.service.impl;

import java.io.InputStream;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chijia.erp.mapper.CustomerMapper;
import com.chijia.erp.model.dto.CustomerDTO;
import com.chijia.erp.model.entity.Customer;
import com.chijia.erp.repository.CustomerRepository;
import com.chijia.erp.service.CustomerService;
import com.chijia.erp.util.ExcelHelper;

@Service
public class CustomerServiceImpl implements CustomerService {

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private CustomerMapper customerMapper;

    @Override
    public List<CustomerDTO> getAllCustomers() {
        return customerRepository.findAll().stream()
                .map(customerMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<CustomerDTO> searchCustomers(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllCustomers();
        }
        String kw = keyword.trim();
        return customerRepository
                .findByShortNameContainingIgnoreCaseOrCustomerCodeContainingIgnoreCaseOrPhoneContainingIgnoreCase(kw, kw, kw)
                .stream()
                .map(customerMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public CustomerDTO getCustomerById(Long id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("找不到客戶，ID: " + id));
        return customerMapper.toDTO(customer);
    }

    @Override
    @Transactional
    public CustomerDTO createCustomer(CustomerDTO dto) {
        if (dto.getCustomerCode() != null && customerRepository.existsByCustomerCode(dto.getCustomerCode())) {
            throw new RuntimeException("客戶編號已存在: " + dto.getCustomerCode());
        }
        Customer customer = customerMapper.toEntity(dto);
        Customer savedCustomer = customerRepository.save(customer);
        return customerMapper.toDTO(savedCustomer);
    }

    @Override
    @Transactional
    public CustomerDTO updateCustomer(Long id, CustomerDTO dto) {
        Customer existing = customerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("找不到客戶，ID: " + id));

        if (dto.getCustomerCode() != null && !dto.getCustomerCode().trim().isEmpty()) {
            existing.setCustomerCode(dto.getCustomerCode().trim());
        }
        existing.setShortName(dto.getShortName());
        existing.setFullName(dto.getFullName());
        existing.setContactPerson(dto.getContactPerson());
        existing.setPhone(dto.getPhone());
        existing.setMobile(dto.getMobile());
        existing.setFax(dto.getFax());
        existing.setTaxId(dto.getTaxId());
        existing.setEmail(dto.getEmail());
        existing.setAddress(dto.getAddress());
        existing.setDeliveryAddress(dto.getDeliveryAddress());
        existing.setCheckoutDay(dto.getCheckoutDay() != null ? dto.getCheckoutDay() : 31);
        existing.setInvoiceType(dto.getInvoiceType());
        existing.setInvoiceTitle(dto.getInvoiceTitle());
        existing.setRemark(dto.getRemark());
        existing.setStatus(dto.isStatus());

        Customer updated = customerRepository.save(existing);
        return customerMapper.toDTO(updated);
    }

    @Override
    @Transactional
    public void toggleStatus(Long id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("找不到客戶，ID: " + id));
        customer.setStatus(!customer.isStatus());
        customerRepository.save(customer);
    }

    @Override
    @Transactional
    public String importCustomersFromExcel(InputStream inputStream) throws Exception {
        int successCount = 0;
        int skipCount = 0;

        Set<String> existingCodes = customerRepository.findAll().stream()
                .map(Customer::getCustomerCode)
                .collect(Collectors.toSet());

        try (Workbook workbook = WorkbookFactory.create(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);

            for (int i = 3; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                String code = ExcelHelper.getCellValueAsString(row.getCell(0));
                String shortName = ExcelHelper.getCellValueAsString(row.getCell(1));

                if (code.isEmpty() || shortName.isEmpty()) continue;

                if (existingCodes.contains(code)) {
                    skipCount++;
                    continue;
                }

                Customer customer = new Customer();
                customer.setCustomerCode(code);
                customer.setShortName(shortName);
                customer.setContactPerson(ExcelHelper.getCellValueAsString(row.getCell(3)));
                customer.setPhone(ExcelHelper.getCellValueAsString(row.getCell(4)));
                customer.setMobile(ExcelHelper.getCellValueAsString(row.getCell(5)));
                customer.setFax(ExcelHelper.getCellValueAsString(row.getCell(6)));
                customer.setFullName(ExcelHelper.getCellValueAsString(row.getCell(14)));
                customer.setTaxId(ExcelHelper.getCellValueAsString(row.getCell(26)));
                customer.setAddress(ExcelHelper.getCellValueAsString(row.getCell(32)));
                customer.setDeliveryAddress(ExcelHelper.getCellValueAsString(row.getCell(36)));
                customer.setEmail(ExcelHelper.getCellValueAsString(row.getCell(39)));
                customer.setRemark(ExcelHelper.getCellValueAsString(row.getCell(40)));
                customer.setInvoiceTitle(ExcelHelper.getCellValueAsString(row.getCell(50)));
                customer.setStatus(true);

                customerRepository.save(customer);
                existingCodes.add(code);
                successCount++;
            }
        }
        return String.format("客戶資料匯入完成！成功匯入 %d 筆，跳過重複 %d 筆。", successCount, skipCount);
    }
}