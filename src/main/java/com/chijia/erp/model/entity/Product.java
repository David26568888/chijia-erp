package com.chijia.erp.model.entity;

import java.math.BigDecimal;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
@Entity
@Table(name = "product")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_code", nullable = false, unique = true, length = 50)
    private String productCode; // 產品編號

    @Column(name = "product_name", nullable = false, length = 150)
    private String productName; // 品名規格

    @Column(name = "barcode", length = 50)
    private String barcode; // 條碼編號

    @Column(name = "unit", length = 20)
    private String unit; // 單位 (例如: 式、支、個)

    // 💡 售價 (改為 BigDecimal)
    @Column(name = "sale_price", precision = 12, scale = 2)
    private BigDecimal salePrice = BigDecimal.ZERO; 

    // 💡 進價/成本 (改為 BigDecimal)
    @Column(name = "cost_price", precision = 12, scale = 2)
    private BigDecimal costPrice = BigDecimal.ZERO;

    // 💡 一般零售改為 Integer，比較與加減更簡單直覺
    @Column(name = "stock_quantity", nullable = false)
    private BigDecimal stockQuantity = BigDecimal.ZERO; // 預設庫存 0.00 (支援小數點後兩位)

    @Column(name = "safety_stock", nullable = false)
    private BigDecimal safetyStock = BigDecimal.ZERO;  // 安全存量 0.00

    @Column(name = "remark", columnDefinition = "TEXT")
    private String remark; // 備註

    @Column(name = "status", nullable = false)
    private boolean status = true; // 狀態 (預設上架)
}