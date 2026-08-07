package com.chijia.erp.service.impl;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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
public class CustomerServiceImpl implements CustomerService{
	
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
	public CustomerDTO getCustomerById(Long id) {
		 Customer customer= customerRepository.findById(id)
				.orElseThrow(()-> new RuntimeException("找不到該客戶，ID:" + id));
		 return customerMapper.toDTO(customer);
	}

	@Override
	@Transactional
	public CustomerDTO creatCustomer(CustomerDTO customerDTO) {
		if(customerRepository.findByCustomerCode(customerDTO.getCustomerCode()).isPresent()) {
			throw new RuntimeException("客戶編號 [" + customerDTO.getCustomerCode() + "] 已存在，無法新增");
		}
		
		Customer customer = customerMapper.toEntity(customerDTO);
		customer.setStatus(true);// 新增預設為啟用
		Customer savedCustomer = customerRepository.save(customer);
		
		return customerMapper.toDTO(savedCustomer);
	}

	@Override
	@Transactional
	public CustomerDTO updateCustomer(Long id, CustomerDTO customerDTO) {
		Customer existingCustomer = customerRepository.findById(id)
				.orElseThrow(() -> new RuntimeException("找不到該客戶，無法更新！ID: " + id));
	
		// 覆蓋前端傳入的新欄位 (ID 保持不動)
		existingCustomer.setShortName(customerDTO.getShortName());
        existingCustomer.setFullName(customerDTO.getFullName());
        existingCustomer.setContactPerson(customerDTO.getContactPerson());
        existingCustomer.setPhone(customerDTO.getPhone());
        existingCustomer.setMobile(customerDTO.getMobile());
        existingCustomer.setTaxId(customerDTO.getTaxId());
        existingCustomer.setCompanyAddress(customerDTO.getCompanyAddress());
        existingCustomer.setCheckoutDay(customerDTO.getCheckoutDay());
        existingCustomer.setInvoiceType(customerDTO.getInvoiceType());
        existingCustomer.setInvoiceTitle(customerDTO.getInvoiceTitle());
        existingCustomer.setRemark(customerDTO.getRemark());
        
        Customer updatCustomer = customerRepository.save(existingCustomer);
        return customerMapper.toDTO(updatCustomer);
	}

	@Override
	@Transactional
	public void toggleStatus(Long id) {
		Customer customer = customerRepository.findById(id)
				.orElseThrow(()-> new RuntimeException("找不到該客戶，無法切換狀態！ID: " + id));
		customer.setStatus(!customer.isStatus());
		customerRepository.save(customer);
		
	}

	@Override
	@Transactional(rollbackFor = Exception.class) // 💡 確保若匯入出錯，整批 Rollback
	public String importCustomersFromExcel(InputStream inputStream) throws Exception {
	    Workbook workbook = WorkbookFactory.create(inputStream);
	    
	    // 讀取客戶資料分頁 "bcust"
	    Sheet sheet = workbook.getSheet("bcust");
	    if (sheet == null) {
	        sheet = workbook.getSheetAt(0);
	    }

	    List<Customer> customerList = new ArrayList<>();
	    int importCount = 0;

	    // 💡 修正 1：資料從第 5 列 (Index = 4) 開始讀取
	    for (int i = 4; i <= sheet.getLastRowNum(); i++) {
	        Row row = sheet.getRow(i);
	        if (row == null) {
	            continue;
	        }

	        String customerCode = ExcelHelper.getCellValueAsString(row.getCell(0));
	        
	        // 💡 修正 2：過濾空白列與星號過路客標記 (* 或 **)
	        if (customerCode.isEmpty() || "*".equals(customerCode) || "**".equals(customerCode)) {
	            continue;
	        }

	        Customer customer = new Customer();
	        customer.setCustomerCode(customerCode);                                   // Col 0: 客戶編號
	        customer.setShortName(ExcelHelper.getCellValueAsString(row.getCell(1))); // Col 1: 客戶簡稱
	        customer.setPhone(ExcelHelper.getCellValueAsString(row.getCell(4)));     // Col 4: 電話1
	        customer.setMobile(ExcelHelper.getCellValueAsString(row.getCell(5)));    // Col 5: 行動電話1
	        customer.setFullName(ExcelHelper.getCellValueAsString(row.getCell(14))); // Col 14: 客戶名稱
	        customer.setTaxId(ExcelHelper.getCellValueAsString(row.getCell(26)));    // Col 26: 統一編號
	        customer.setCompanyAddress(ExcelHelper.getCellValueAsString(row.getCell(32))); // Col 32: 公司地址
	        customer.setStatus(true);

	        // 防重複匯入：利用客戶編號檢查
	        Optional<Customer> existingCustomerOpt = customerRepository.findByCustomerCode(customerCode);
	        if (existingCustomerOpt.isPresent()) {
	            Customer existingCustomer = existingCustomerOpt.get();
	            existingCustomer.setShortName(customer.getShortName());
	            existingCustomer.setPhone(customer.getPhone());
	            existingCustomer.setMobile(customer.getMobile());
	            existingCustomer.setFullName(customer.getFullName());
	            existingCustomer.setTaxId(customer.getTaxId());
	            existingCustomer.setCompanyAddress(customer.getCompanyAddress());
	            customerList.add(existingCustomer);
	        } else {
	            customerList.add(customer);
	        }

	        importCount++;
	    }

	    if (!customerList.isEmpty()) {
	        customerRepository.saveAll(customerList);
	    }

	    workbook.close();
	    return "成功匯入/更新 " + importCount + " 筆客戶資料！";
	}

}
