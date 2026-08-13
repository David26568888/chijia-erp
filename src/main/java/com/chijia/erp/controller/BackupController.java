package com.chijia.erp.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.chijia.erp.api.ApiResponse;
import com.chijia.erp.model.dto.CustomerDTO;
import com.chijia.erp.model.dto.ProductDTO;
import com.chijia.erp.model.dto.PurchaseOrderDTO;
import com.chijia.erp.model.dto.SaleOrderDTO;
import com.chijia.erp.model.dto.SupplierDTO;
import com.chijia.erp.service.CustomerService;
import com.chijia.erp.service.ProductService;
import com.chijia.erp.service.PurchaseOrderService;
import com.chijia.erp.service.SaleOrderService;
import com.chijia.erp.service.SupplierService;
import com.chijia.erp.util.ExcelBackupUtil;

@RestController
@RequestMapping("/api/v1/backup")
@CrossOrigin(origins = "*")
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

    // 💡 1. 一鍵匯出備份檔 API
    @GetMapping("/export/{type}")
    public ResponseEntity<byte[]> exportBackup(@PathVariable("type") String type) {
        try {
            byte[] data;
            String fileName;

            switch (type.toLowerCase()) {
                case "products":
                    List<ProductDTO> products = productService.getAllProducts();
                    data = ExcelBackupUtil.exportProducts(products);
                    fileName = "奇家五金_商品資料備份.xlsx";
                    break;
                case "suppliers":
                    List<SupplierDTO> suppliers = supplierService.getAllSuppliers();
                    data = ExcelBackupUtil.exportSuppliers(suppliers);
                    fileName = "奇家五金_廠商資料備份.xlsx";
                    break;
                case "customers":
                    List<CustomerDTO> customers = customerService.getAllCustomers();
                    data = ExcelBackupUtil.exportCustomers(customers);
                    fileName = "奇家五金_客戶資料備份.xlsx";
                    break;
                case "purchases":
                    List<PurchaseOrderDTO> purchases = purchaseOrderService.getAllPurchaseOrders();
                    data = ExcelBackupUtil.exportPurchaseOrders(purchases);
                    fileName = "奇家五金_進貨單備份.xlsx";
                    break;
                case "sales":
                    List<SaleOrderDTO> sales = saleOrderService.getAllSaleOrders();
                    data = ExcelBackupUtil.exportSaleOrders(sales);
                    fileName = "奇家五金_銷貨單備份.xlsx";
                    break;
                default:
                    return ResponseEntity.badRequest().build();
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", new String(fileName.getBytes("UTF-8"), "ISO-8859-1"));

            return new ResponseEntity<>(data, headers, HttpStatus.OK);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // 💡 2. 一鍵匯入還原 API (商品)
    @PostMapping("/import/products")
    public ResponseEntity<ApiResponse<String>> importProducts(@RequestParam("file") MultipartFile file) {
        try {
            String msg = productService.importProductsFromExcel(file.getInputStream());
            return ResponseEntity.ok(ApiResponse.success("商品資料還原成功！", msg));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.error(500, "還原失敗: " + e.getMessage()));
        }
    }
}