package com.chijia.erp.model.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class ProductHistoryDTO {

    private List<PurchaseRecordDTO> purchaseHistory; // 進貨歷史紀錄
    private List<SaleRecordDTO> saleHistory;         // 銷售歷史紀錄

    public ProductHistoryDTO() {}

    public ProductHistoryDTO(List<PurchaseRecordDTO> purchaseHistory, List<SaleRecordDTO> saleHistory) {
        this.purchaseHistory = purchaseHistory;
        this.saleHistory = saleHistory;
    }

 // ================= 1. 進貨歷史內部類別 =================
    public static class PurchaseRecordDTO {
        private String supplierName;   // 進貨廠商
        private LocalDate purchaseDate;// 進貨日期
        private BigDecimal unitPrice;  // 進貨單價
        private BigDecimal quantity;      // 數量

     // 💡 補上無參數建構子
        public PurchaseRecordDTO() {}

        // 💡 補上全參數建構子
        public PurchaseRecordDTO(LocalDate purchaseDate, String supplierName, BigDecimal unitPrice, BigDecimal quantity) {
            this.purchaseDate = purchaseDate;
            this.supplierName = supplierName;
            this.unitPrice = unitPrice;
            this.quantity = quantity;
        }

        public String getSupplierName() { return supplierName; }
        public void setSupplierName(String supplierName) { this.supplierName = supplierName; }

        public LocalDate getPurchaseDate() { return purchaseDate; }
        public void setPurchaseDate(LocalDate purchaseDate) { this.purchaseDate = purchaseDate; }

        public BigDecimal getUnitPrice() { return unitPrice; }
        public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }

        public BigDecimal getQuantity() { return quantity; }
        public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
    }

 // ================= 2. 銷售歷史內部類別 =================
    public static class SaleRecordDTO {
        private String customerName; // 銷售客戶
        private LocalDate saleDate;  // 銷貨日期
        private BigDecimal unitPrice;// 銷貨單價
        private BigDecimal quantity;    // 數量

     // 💡 補上無參數建構子
        public SaleRecordDTO() {}

        // 💡 補上全參數建構子
        public SaleRecordDTO(LocalDate saleDate, String customerName, BigDecimal unitPrice, BigDecimal quantity) {
            this.saleDate = saleDate;
            this.customerName = customerName;
            this.unitPrice = unitPrice;
            this.quantity = quantity;
        }

        public String getCustomerName() { return customerName; }
        public void setCustomerName(String customerName) { this.customerName = customerName; }

        public LocalDate getSaleDate() { return saleDate; }
        public void setSaleDate(LocalDate saleDate) { this.saleDate = saleDate; }

        public BigDecimal getUnitPrice() { return unitPrice; }
        public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }

        public BigDecimal getQuantity() { return quantity; }
        public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
    }

    public List<PurchaseRecordDTO> getPurchaseHistory() { return purchaseHistory; }
    public void setPurchaseHistory(List<PurchaseRecordDTO> purchaseHistory) { this.purchaseHistory = purchaseHistory; }
    
    public List<SaleRecordDTO> getSaleHistory() { return saleHistory; }
    public void setSaleHistory(List<SaleRecordDTO> saleHistory) { this.saleHistory = saleHistory; }
}