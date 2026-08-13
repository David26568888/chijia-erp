package com.chijia.erp.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.chijia.erp.model.dto.CustomerDTO;
import com.chijia.erp.model.dto.ProductDTO;
import com.chijia.erp.model.dto.PurchaseOrderDTO;
import com.chijia.erp.model.dto.SaleOrderDTO;
import com.chijia.erp.model.dto.SupplierDTO;

public class ExcelBackupUtil {

    // 1. 匯出商品資料表
    public static byte[] exportProducts(List<ProductDTO> products) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("商品資料");
            createHeaderRow(sheet, new String[]{
                "商品編號", "國際條碼", "商品名稱", "單位", "零售價", "基準成本", "最新進價", "平均成本", "庫存數量", "安全存量", "狀態"
            });

            int rowIdx = 1;
            for (ProductDTO p : products) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(p.getProductCode() != null ? p.getProductCode() : "");
                row.createCell(1).setCellValue(p.getBarcode() != null ? p.getBarcode() : "");
                row.createCell(2).setCellValue(p.getProductName() != null ? p.getProductName() : "");
                row.createCell(3).setCellValue(p.getUnit() != null ? p.getUnit() : "個");
                row.createCell(4).setCellValue(p.getSalePrice() != null ? p.getSalePrice().doubleValue() : 0);
                row.createCell(5).setCellValue(p.getCostPrice() != null ? p.getCostPrice().doubleValue() : 0);
                row.createCell(6).setCellValue(p.getLastCostPrice() != null ? p.getLastCostPrice().doubleValue() : 0);
                row.createCell(7).setCellValue(p.getAvgCostPrice() != null ? p.getAvgCostPrice().doubleValue() : 0);
                row.createCell(8).setCellValue(p.getStockQuantity() != null ? p.getStockQuantity().doubleValue() : 0);
                row.createCell(9).setCellValue(p.getSafetyStock() != null ? p.getSafetyStock().doubleValue() : 5);
                row.createCell(10).setCellValue(p.isStatus() ? "上架" : "停用");
            }

            workbook.write(out);
            return out.toByteArray();
        }
    }

    // 2. 匯出廠商資料表
    public static byte[] exportSuppliers(List<SupplierDTO> suppliers) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("廠商資料");
            createHeaderRow(sheet, new String[]{"廠商編號", "廠商簡稱", "廠商全名", "電話", "聯絡人", "公司地址"});

            int rowIdx = 1;
            for (SupplierDTO s : suppliers) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(s.getSupplierCode() != null ? s.getSupplierCode() : "");
                row.createCell(1).setCellValue(s.getShortName() != null ? s.getShortName() : "");
                row.createCell(2).setCellValue(s.getFullName() != null ? s.getFullName() : "");
                row.createCell(3).setCellValue(s.getPhone() != null ? s.getPhone() : "");
                row.createCell(4).setCellValue(s.getContactPerson() != null ? s.getContactPerson() : "");
                row.createCell(5).setCellValue(s.getCompanyAddress()!= null ? s.getCompanyAddress() : "");
            }

            workbook.write(out);
            return out.toByteArray();
        }
    }

    // 3. 匯出客戶資料表
    public static byte[] exportCustomers(List<CustomerDTO> customers) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("客戶資料");
            createHeaderRow(sheet, new String[]{"客戶編號", "客戶簡稱", "客戶全名", "電話", "聯絡人", "地址"});

            int rowIdx = 1;
            for (CustomerDTO c : customers) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(c.getCustomerCode() != null ? c.getCustomerCode() : "");
                row.createCell(1).setCellValue(c.getShortName() != null ? c.getShortName() : "");
                row.createCell(2).setCellValue(c.getFullName() != null ? c.getFullName() : "");
                row.createCell(3).setCellValue(c.getPhone() != null ? c.getPhone() : "");
                row.createCell(4).setCellValue(c.getContactPerson() != null ? c.getContactPerson() : "");
                row.createCell(5).setCellValue(c.getAddress() != null ? c.getAddress() : "");
            }

            workbook.write(out);
            return out.toByteArray();
        }
    }

    // 4. 匯出進貨單歷史備份
    public static byte[] exportPurchaseOrders(List<PurchaseOrderDTO> orders) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("進貨單備份");
            createHeaderRow(sheet, new String[]{"進貨單號", "進貨日期", "廠商名稱", "整單折讓", "應付總金額", "備註"});

            int rowIdx = 1;
            for (PurchaseOrderDTO po : orders) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(po.getPurchaseNo() != null ? po.getPurchaseNo() : "");
                row.createCell(1).setCellValue(po.getPurchaseDate() != null ? po.getPurchaseDate().toString() : "");
                row.createCell(2).setCellValue(po.getSupplierName() != null ? po.getSupplierName() : "");
                row.createCell(3).setCellValue(po.getDiscountAmount() != null ? po.getDiscountAmount().doubleValue() : 0);
                row.createCell(4).setCellValue(po.getTotalAmount() != null ? po.getTotalAmount().doubleValue() : 0);
                row.createCell(5).setCellValue(po.getRemark() != null ? po.getRemark() : "");
            }

            workbook.write(out);
            return out.toByteArray();
        }
    }

    // 5. 匯出銷貨單歷史備份
    public static byte[] exportSaleOrders(List<SaleOrderDTO> orders) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("銷貨單備份");
            createHeaderRow(sheet, new String[]{"銷貨單號", "銷貨日期", "客戶名稱", "整單折讓", "實收總金額", "備註"});

            int rowIdx = 1;
            for (SaleOrderDTO so : orders) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(so.getSaleNo() != null ? so.getSaleNo() : "");
                row.createCell(1).setCellValue(so.getSaleDate() != null ? so.getSaleDate().toString() : "");
                row.createCell(2).setCellValue(so.getCustomerName() != null ? so.getCustomerName() : "門市散客");
                row.createCell(3).setCellValue(so.getDiscountAmount() != null ? so.getDiscountAmount().doubleValue() : 0);
                row.createCell(4).setCellValue(so.getTotalAmount() != null ? so.getTotalAmount().doubleValue() : 0);
                row.createCell(5).setCellValue(so.getRemark() != null ? so.getRemark() : "");
            }

            workbook.write(out);
            return out.toByteArray();
        }
    }

    // 表頭樣式美化輔助方法
    private static void createHeaderRow(Sheet sheet, String[] headers) {
        Row headerRow = sheet.createRow(0);
        Workbook workbook = sheet.getWorkbook();
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);

        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(style);
            sheet.setColumnWidth(i, 20 * 256); // 設定預設欄寬
        }
    }
}