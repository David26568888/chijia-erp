package com.chijia.erp.controller;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.chijia.erp.model.dto.SaleOrderDTO;
import com.chijia.erp.model.entity.SaleOrderItem;
import com.chijia.erp.repository.SaleOrderItemRepository;
import com.chijia.erp.repository.SaleOrderRepository;
import com.chijia.erp.service.SaleOrderService;

@RestController
@RequestMapping("/api/v1/reports")
public class FinanceReportController {

    @Autowired
    private SaleOrderRepository saleOrderRepository;
    
    @Autowired
    private SaleOrderItemRepository saleOrderItemRepository;

    @Autowired
    private SaleOrderService saleOrderService;

    // 1. 查詢單一訂單賺多少錢 (訂單毛利分析)
    // GET /api/v1/reports/order/{id}/profit
    @GetMapping("/order/{id}/profit")
    public ResponseEntity<Map<String, Object>> getOrderProfit(@PathVariable Long id) {
        SaleOrderDTO order = saleOrderService.getSaleOrderById(id);
        
        BigDecimal totalCost = BigDecimal.ZERO;
        BigDecimal totalGrossProfit = BigDecimal.ZERO;

        if (order.getItems() != null) {
            for (SaleOrderDTO.ItemDTO item : order.getItems()) {
                if (item.getTotalCost() != null) {
                    totalCost = totalCost.add(item.getTotalCost());
                }
                if (item.getGrossProfit() != null) {
                    totalGrossProfit = totalGrossProfit.add(item.getGrossProfit());
                }
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("saleNo", order.getSaleNo());
        result.put("saleDate", order.getSaleDate());
        result.put("totalAmount", order.getTotalAmount());
        result.put("totalCost", totalCost);
        result.put("grossProfit", totalGrossProfit); // 這筆訂單淨賺多少！

        return ResponseEntity.ok(result);
    }

    // 2. 查詢指定月份的總營收與總毛利
    // GET /api/reports/monthly?year=2026&month=6
    @GetMapping("/monthly")
    public ResponseEntity<Map<String, Object>> getMonthlyReport(
            @RequestParam int year, @RequestParam int month) {
        
        BigDecimal monthlyRevenue = saleOrderRepository.sumTotalAmountByMonth(year, month);
        BigDecimal monthlyProfit = saleOrderRepository.sumGrossProfitByMonth(year, month);

        Map<String, Object> result = new HashMap<>();
        result.put("year", year);
        result.put("month", month);
        result.put("totalRevenue", monthlyRevenue != null ? monthlyRevenue : BigDecimal.ZERO);
        result.put("totalGrossProfit", monthlyProfit != null ? monthlyProfit : BigDecimal.ZERO);

        return ResponseEntity.ok(result);
    }

    // 3. 查詢最賺錢商品排行榜 並取前 100 名
    // GET /api/v1/reports/top-products
    @GetMapping("/top-products")
    public ResponseEntity<List<Object[]>> getTopProfitableProducts() {
        List<Object[]> topProducts = saleOrderItemRepository.findTopProfitableProducts(PageRequest.of(0, 100));
        return ResponseEntity.ok(topProducts);
    }

    // 4. 查詢賣得最多的商品排行榜  並取前 100 名
    // GET /api/v1/reports/best-sellers
    @GetMapping("/best-sellers")
    public ResponseEntity<List<Object[]>> getBestSellingProducts() {
        List<Object[]> bestSellers = saleOrderItemRepository.findTopSellingProducts(PageRequest.of(0, 100));
        return ResponseEntity.ok(bestSellers);
    }
}