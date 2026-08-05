package com.chijia.erp.model.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "product")
@Data
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_code", nullable = false, unique = true, length = 50)
    private String productCode; // 商品編號

    @Column(name = "product_name", nullable = false, length = 100)
    private String productName; // 品名規格

    @Column(name = "barcode", length = 50)
    private String barcode; // 條碼

    @Column(name = "unit", length = 20)
    private String unit; // 單位 (如：個、包、盒、尺)

    @Column(name = "sale_price", precision = 12, scale = 2)
    private BigDecimal salePrice; // 零售售價

    // 💡 三軌成本 1: 預設 / 基準成本
    @Column(name = "cost_price", precision = 12, scale = 2)
    private BigDecimal costPrice;

    // 💡 三軌成本 2: 最後進價 (每次進貨單建立時直接覆蓋)
    @Column(name = "last_cost_price", precision = 12, scale = 2)
    private BigDecimal lastCostPrice;

    // 💡 三軌成本 3: 移動加權平均成本 (每次進貨單建立時按數量與金額加權重算)
    @Column(name = "avg_cost_price", precision = 12, scale = 2)
    private BigDecimal avgCostPrice;

    @Column(name = "stock_quantity", precision = 12, scale = 2)
    private BigDecimal stockQuantity = BigDecimal.ZERO; // 現有庫存數量

    @Column(name = "safety_stock", precision = 12, scale = 2)
    private BigDecimal safetyStock = BigDecimal.ZERO; // 安全存量

    @Column(name = "status", nullable = false)
    private boolean status = true; // 狀態 (true: 啟用, false: 停用)

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.stockQuantity == null) this.stockQuantity = BigDecimal.ZERO;
        if (this.safetyStock == null) this.safetyStock = BigDecimal.ZERO;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}