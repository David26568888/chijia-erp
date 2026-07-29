package com.chijia.erp.service.impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chijia.erp.model.dto.CreateSaleOrderDTO;
import com.chijia.erp.model.dto.SaleOrderDTO;
import com.chijia.erp.model.entity.Product;
import com.chijia.erp.model.entity.SaleOrder;
import com.chijia.erp.model.entity.SaleOrderItem;
import com.chijia.erp.repository.CustomerRepository;
import com.chijia.erp.repository.ProductRepository;
import com.chijia.erp.repository.SaleOrderRepository;
import com.chijia.erp.service.SaleOrderService;

@Service
public class SaleOrderServiceImpl implements SaleOrderService {

	@Autowired
	private SaleOrderRepository saleOrderRepository;
	
	@Autowired
	private ProductRepository productRepository;
	
	@Autowired
	private CustomerRepository customerRepository;
	
	@Override
	@Transactional // 💡 關鍵：資料庫事務管理，若中途有任何 Exception 發生，會自動全數回滾！
	public SaleOrderDTO createSaleOrder(CreateSaleOrderDTO createDTO) {
		// 1. 驗證客戶是否存在
		if (createDTO.getCustomerId() != null && !customerRepository.existsById(createDTO.getCustomerId())) {
			throw new RuntimeException("找不到對應的客戶 ID: " + createDTO.getCustomerId());
		}
		
		// 2. 建立銷貨單主檔
		SaleOrder saleOrder = new SaleOrder();
		saleOrder.setOrderNo(generateOrderNo());
		saleOrder.setCustomerId(createDTO.getCustomerId());
		saleOrder.setRemark(createDTO.getRemark());
		
		BigDecimal grandTotal = BigDecimal.ZERO;
		
		// 3. 處理每筆銷貨明細 & 扣減庫存
		if (createDTO.getItems() != null) {
			for (CreateSaleOrderDTO.CreateItemDTO itemDTO : createDTO.getItems()) {
				// 3a. 檢查商品是否存在
				Product product = productRepository.findById(itemDTO.getProductId())
									.orElseThrow(() -> new RuntimeException("商品不存在，ID: " + itemDTO.getProductId()));
				
				int buyQty = itemDTO.getQuantity() != null ? itemDTO.getQuantity() : 0;

				// 3b. 檢查庫存是否足夠 (修正為 < 小於)
				if (product.getStockQuantity() < buyQty) {
					throw new RuntimeException("商品 [" + product.getProductName() + "] 庫存不足！目前庫存: "
							+ product.getStockQuantity() + "，欲購買數量: " + buyQty);
				}
				
				// 3c. 扣減商品庫存！
				product.setStockQuantity(product.getStockQuantity() - buyQty);
				productRepository.save(product); // 更新商品庫存
				
				// 3d. 建立銷貨明細 Entity
				SaleOrderItem orderItem = new SaleOrderItem();
				orderItem.setProductId(product.getId());
				orderItem.setProductName(product.getProductName());
				orderItem.setQuantity(buyQty);
				
				// 取得商品售價
				BigDecimal unitPrice = product.getSalePrice() != null ? product.getSalePrice() : BigDecimal.ZERO;
				orderItem.setUnitPrice(unitPrice);
				
				BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(buyQty));
				orderItem.setSubtotal(subtotal);
	            
				// 累加總金額
				grandTotal = grandTotal.add(subtotal);
	            
				// 雙向關聯加入主檔
				saleOrder.addItem(orderItem);
			}
		}
		
		saleOrder.setTotalAmount(grandTotal);
		
		// 4. 存入資料庫 (連同明細一同寫入)
		SaleOrder savedOrder = saleOrderRepository.save(saleOrder);

		// 5. 轉為 SaleOrderDTO 回傳
		return convertToDTO(savedOrder);
	}

	// Entity 轉 DTO
	private SaleOrderDTO convertToDTO(SaleOrder entity) {
		SaleOrderDTO dto = new SaleOrderDTO();
		dto.setId(entity.getId());
		dto.setOrderNo(entity.getOrderNo());
		dto.setCustomerId(entity.getCustomerId());
		dto.setTotalAmount(entity.getTotalAmount());
		dto.setOrderDate(entity.getOrderDate());
		dto.setRemark(entity.getRemark());

		List<SaleOrderDTO.ItemDTO> itemDTOs = new ArrayList<>();
		if (entity.getItems() != null) {
			for (SaleOrderItem item : entity.getItems()) {
				SaleOrderDTO.ItemDTO itemDTO = new SaleOrderDTO.ItemDTO();
				itemDTO.setId(item.getId());
				itemDTO.setProductId(item.getProductId());
				itemDTO.setProductName(item.getProductName());
				itemDTO.setQuantity(item.getQuantity());
				itemDTO.setUnitPrice(item.getUnitPrice());
				itemDTO.setSubtotal(item.getSubtotal());
				itemDTOs.add(itemDTO);
			}
		}
		dto.setItems(itemDTOs);
		return dto;
	}

	// 💡 自動生成銷貨單號邏輯 (格式：SO-YYYYMMDD-隨機4碼)
	private String generateOrderNo() {
		String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
		String randomStr = UUID.randomUUID().toString().substring(0, 4).toUpperCase();
		
		return "SO-" + dateStr + "-" + randomStr;
	}

	@Override
	public SaleOrderDTO getSaleOrderById(Long id) {
		SaleOrder saleOrder = saleOrderRepository.findById(id)
				.orElseThrow(() -> new RuntimeException("找不到銷貨單，ID: " + id));
		return convertToDTO(saleOrder);
	}
}