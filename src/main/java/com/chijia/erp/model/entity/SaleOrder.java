package com.chijia.erp.model.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "sale_order")
public class SaleOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sale_no", nullable = false, unique = true, length = 30)
    private String saleNo; // 銷貨單號 (例如: SO-20260813-XXXX)

    @Column(name = "customer_id")
    private Long customerId; // 客戶 ID (允許為 null 代表散客)

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO; // 實收總金額

    @Column(name = "discount_amount", precision = 12, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO; // 💡 整單折讓/折扣金額

    @Column(name = "sale_date", nullable = false)
    private LocalDate saleDate; // 銷貨日期

    private String remark; // 銷貨備註

    @OneToMany(mappedBy = "saleOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SaleOrderItem> items = new ArrayList<>();

    public void addItem(SaleOrderItem item) {
        items.add(item);
        item.setSaleOrder(this);
    }

    @PrePersist
    public void onCreate() {
        if (this.saleDate == null) {
            this.saleDate = LocalDate.now();
        }
    }
}