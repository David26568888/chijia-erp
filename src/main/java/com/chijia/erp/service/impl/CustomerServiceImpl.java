package com.chijia.erp.service.impl;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chijia.erp.mapper.CustomerMapper;
import com.chijia.erp.model.dto.CustomerDTO;
import com.chijia.erp.model.entity.Customer;
import com.chijia.erp.repository.CustomerRepository;
import com.chijia.erp.service.CustomerService;


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

}
