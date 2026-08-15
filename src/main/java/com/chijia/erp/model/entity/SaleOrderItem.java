package com.chijia.erp.model.entity;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "sale_order_item")
public class SaleOrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sale_order_id", nullable = false)
    private SaleOrder saleOrder;

    @Column(name = "product_id", nullable = false)
    private Long productId; // 商品 ID

    @Column(name = "product_code")
    private String productCode; // 💡 備份商品編號 (例如: 0-0000)

    @Column(name = "product_name")
    private String productName; // 備份商品名稱

    @Column(name = "quantity", nullable = false, precision = 12, scale = 2)
    private BigDecimal quantity = BigDecimal.ZERO; // 銷貨數量

    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice = BigDecimal.ZERO; // 銷售單價

    @Column(name = "subtotal", nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal = BigDecimal.ZERO; // 項目小計
    
 // 在 SaleOrderItem.java 中新增以下欄位：
    @Column(name = "cost_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal costPrice = BigDecimal.ZERO; // 💡 銷售當下的單位成本

    @Column(name = "total_cost", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalCost = BigDecimal.ZERO; // 💡 項目總成本 (costPrice * quantity)

    @Column(name = "gross_profit", nullable = false, precision = 12, scale = 2)
    private BigDecimal grossProfit = BigDecimal.ZERO; // 💡 項目毛利 (subtotal - totalCost)
}