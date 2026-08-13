package com.chijia.erp.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.chijia.erp.api.ApiResponse;
import com.chijia.erp.service.CustomerService;
import com.chijia.erp.service.ProductService;
import com.chijia.erp.service.PurchaseOrderService;
import com.chijia.erp.service.SaleOrderService;
import com.chijia.erp.service.SupplierService;
import com.chijia.erp.util.ExcelBackupUtil;

@RestController
@RequestMapping("/api/v1/backup")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class BackupController {

    @Autowired
    private ProductService productService;

    @Autowired
    private SupplierService supplierService;

    @Autowired
    private CustomerService customerService;

    @Autowired
    private PurchaseOrderService purchaseOrderService;

    @Autowired
    private SaleOrderService saleOrderService;

    // ==================== 1. 匯出備份 (Export) ====================
    @GetMapping("/export/{type}")
    public ResponseEntity<byte[]> exportBackup(@PathVariable String type) {
        try {
            byte[] data;
            String filename;

            switch (type) {
                case "products":
                    data = ExcelBackupUtil.exportProducts(productService.getAllProducts());
                    filename = "奇家五金_商品與三軌成本_備份.xlsx";
                    break;
                case "suppliers":
                    data = ExcelBackupUtil.exportSuppliers(supplierService.getAllSuppliers());
                    filename = "奇家五金_廠商資料_備份.xlsx";
                    break;
                case "customers":
                    data = ExcelBackupUtil.exportCustomers(customerService.getAllCustomers());
                    filename = "奇家五金_客戶資料_備份.xlsx";
                    break;
                case "purchases":
                    data = ExcelBackupUtil.exportPurchaseOrders(purchaseOrderService.getAllPurchaseOrders());
                    filename = "奇家五金_進貨單歷史_備份.xlsx";
                    break;
                case "sales":
                    data = ExcelBackupUtil.exportSaleOrders(saleOrderService.getAllSaleOrders());
                    filename = "奇家五金_銷貨單歷史_備份.xlsx";
                    break;
                default:
                    return ResponseEntity.badRequest().build();
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", new String(filename.getBytes("UTF-8"), "ISO-8859-1"));

            return ResponseEntity.ok().headers(headers).body(data);
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }

    // ==================== 2. 災難還原 / 匯入 (Import) ====================

    // 💡 2A. 商品：還原「系統自身標準備份檔」(11欄)
    @PostMapping("/import/products/backup")
    public ResponseEntity<ApiResponse<String>> restoreProductsBackup(@RequestParam("file") MultipartFile file) {
        try {
            String resultMsg = productService.restoreProductsFromBackup(file.getInputStream());
            return ResponseEntity.ok(ApiResponse.success("商品備份還原成功！", resultMsg));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.error(500, "還原失敗: " + e.getMessage()));
        }
    }

    // 💡 2B. 商品：匯入「舊系統原始大張報表」(100多欄)
    @PostMapping("/import/products/raw")
    public ResponseEntity<ApiResponse<String>> importProductsRaw(@RequestParam("file") MultipartFile file) {
        try {
            String resultMsg = productService.importProductsFromExcel(file.getInputStream());
            return ResponseEntity.ok(ApiResponse.success("原始商品報表匯入成功！", resultMsg));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.error(500, "匯入失敗: " + e.getMessage()));
        }
    }

    // 💡 2C-1. 廠商：還原「系統自身備份檔」
    @PostMapping("/import/suppliers/backup")
    public ResponseEntity<ApiResponse<String>> restoreSuppliersBackup(@RequestParam("file") MultipartFile file) {
        try {
            String resultMsg = supplierService.restoreSuppliersFromBackup(file.getInputStream());
            return ResponseEntity.ok(ApiResponse.success("廠商備份還原成功！", resultMsg));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.error(500, "還原失敗: " + e.getMessage()));
        }
    }

    // 💡 2C-2. 廠商：匯入「舊系統原始報表」
    @PostMapping("/import/suppliers/raw")
    public ResponseEntity<ApiResponse<String>> importSuppliersRaw(@RequestParam("file") MultipartFile file) {
        try {
            String resultMsg = supplierService.importSuppliersFromExcel(file.getInputStream());
            return ResponseEntity.ok(ApiResponse.success("原始廠商報表匯入成功！", resultMsg));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.error(500, "匯入失敗: " + e.getMessage()));
        }
    }

    // 💡 2D-1. 客戶：還原「系統自身備份檔」
    @PostMapping("/import/customers/backup")
    public ResponseEntity<ApiResponse<String>> restoreCustomersBackup(@RequestParam("file") MultipartFile file) {
        try {
            String resultMsg = customerService.restoreCustomersFromBackup(file.getInputStream());
            return ResponseEntity.ok(ApiResponse.success("客戶備份還原成功！", resultMsg));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.error(500, "還原失敗: " + e.getMessage()));
        }
    }

    // 💡 2D-2. 客戶：匯入「舊系統原始報表」
    @PostMapping("/import/customers/raw")
    public ResponseEntity<ApiResponse<String>> importCustomersRaw(@RequestParam("file") MultipartFile file) {
        try {
            String resultMsg = customerService.importCustomersFromExcel(file.getInputStream());
            return ResponseEntity.ok(ApiResponse.success("原始客戶報表匯入成功！", resultMsg));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.error(500, "匯入失敗: " + e.getMessage()));
        }
    }
}