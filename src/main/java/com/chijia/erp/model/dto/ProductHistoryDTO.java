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

    // 進貨單筆明細 DTO
    public static class PurchaseRecordDTO {
        private String supplierName;   // 進貨廠商
        private LocalDate purchaseDate;// 進貨日期
        private BigDecimal unitPrice;  // 進貨單價
        private BigDecimal quantity;      // 數量

        public PurchaseRecordDTO(String supplierName, LocalDate purchaseDate, BigDecimal unitPrice, BigDecimal quantity) {
            this.supplierName = supplierName;
            this.purchaseDate = purchaseDate;
            this.unitPrice = unitPrice;
            this.quantity = quantity;
        }

        public String getSupplierName() { return supplierName; }
        public LocalDate getPurchaseDate() { return purchaseDate; }
        public BigDecimal getUnitPrice() { return unitPrice; }
        public BigDecimal getQuantity() { return quantity; }
    }

    // 銷貨單筆明細 DTO
    public static class SaleRecordDTO {
        private String customerName; // 銷售客戶
        private LocalDate saleDate;  // 銷貨日期
        private BigDecimal unitPrice;// 銷貨單價
        private BigDecimal quantity;    // 數量

        public SaleRecordDTO(String customerName, LocalDate saleDate, BigDecimal unitPrice, BigDecimal quantity) {
            this.customerName = customerName;
            this.saleDate = saleDate;
            this.unitPrice = unitPrice;
            this.quantity = quantity;
        }

        public String getCustomerName() { return customerName; }
        public LocalDate getSaleDate() { return saleDate; }
        public BigDecimal getUnitPrice() { return unitPrice; }
        public BigDecimal getQuantity() { return quantity; }
    }

    public List<PurchaseRecordDTO> getPurchaseHistory() { return purchaseHistory; }
    public void setPurchaseHistory(List<PurchaseRecordDTO> purchaseHistory) { this.purchaseHistory = purchaseHistory; }
    public List<SaleRecordDTO> getSaleHistory() { return saleHistory; }
    public void setSaleHistory(List<SaleRecordDTO> saleHistory) { this.saleHistory = saleHistory; }
}