package com.chijia.erp.model.entity;

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

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "customer")
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_code", nullable = false, unique = true, length = 50)
    private String customerCode; // 客戶編號（例如：0002、1）

    @Column(name = "short_name", length = 50)
    private String shortName; // 客戶簡稱（例如：林建華、林天來鴻達）[cite: 16]

    @Column(name = "full_name", length = 100)
    private String fullName; // 客戶名稱/全稱[cite: 16]

    @Column(name = "contact_person", length = 50)
    private String contactPerson; // 聯絡人[cite: 16]

    @Column(name = "phone", length = 20)
    private String phone; // 電話[cite: 16]

    @Column(name = "mobile", length = 20)
    private String mobile; // 行動電話[cite: 16]

    @Column(name = "fax", length = 20)
    private String fax; // 💡 傳真機

    @Column(name = "tax_id", length = 20)
    private String taxId; // 統一編號[cite: 16]

    @Column(name = "email", length = 100)
    private String email; // 💡 電子信箱

    @Column(name = "address", length = 255)
    private String address; // 💡 公司地址 (統稱為 address)

    @Column(name = "delivery_address", length = 255)
    private String deliveryAddress; // 送貨地址 / 工地地址

    @Column(name = "checkout_day")
    private Integer checkoutDay = 31; // 結帳日，預設 31 日[cite: 16]

    @Column(name = "invoice_type", length = 50)
    private String invoiceType; // 發票類別 (例如: 三聯式)[cite: 16]

    @Column(name = "invoice_title", length = 100)
    private String invoiceTitle; // 發票抬頭[cite: 16]

    @Column(name = "remark", columnDefinition = "TEXT")
    private String remark; // 備註[cite: 16]

    @Column(name = "status", nullable = false)
    private boolean status = true; // 狀態 (true: 啟用, false: 停用)[cite: 16]
}