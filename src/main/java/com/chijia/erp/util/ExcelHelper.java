package com.chijia.erp.util;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import lombok.extern.slf4j.Slf4j;

@Slf4j // 💡 使用 Lombok 提供的日誌工具，替換 System.err
public class ExcelHelper {

    /**
     * 安全獲取儲存格字串值 (防範空值與科學記號)
     */
    public static String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return "";
        }

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                }
                // 避免統編或電話被轉成科學記號 (如 1.23E10)
                DecimalFormat dFormat = new DecimalFormat("#.##");
                return dFormat.format(cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                try {
                    return cell.getStringCellValue().trim();
                } catch (Exception e) {
                    try {
                        DecimalFormat df = new DecimalFormat("#.##");
                        return df.format(cell.getNumericCellValue());
                    } catch (Exception ex) {
                        return "";
                    }
                }
            case BLANK:
            default:
                return "";
        }
    }

    /**
     * 💡 金融/成本專用：安全地解析儲存格並轉為 BigDecimal (支援公式與字串數字轉換)
     */
    public static BigDecimal getCellValueAsBigDecimal(Cell cell, BigDecimal defaultValue) {
        if (cell == null) {
            return defaultValue;
        }

        try {
            switch (cell.getCellType()) {
                case NUMERIC:
                    return BigDecimal.valueOf(cell.getNumericCellValue());
                    
                case STRING:
                    String val = cell.getStringCellValue().trim();
                    return val.isEmpty() ? defaultValue : new BigDecimal(val);
                    
                case FORMULA:
                    // 💡 嘗試抓取 Excel 公式計算出來的數值結果
                    try {
                        return BigDecimal.valueOf(cell.getNumericCellValue());
                    } catch (Exception e) {
                        String formulaVal = cell.getStringCellValue().trim();
                        return formulaVal.isEmpty() ? defaultValue : new BigDecimal(formulaVal);
                    }
                    
                default:
                    return defaultValue;
            }
        } catch (Exception e) {
            // 💡 使用專業日誌紀錄錯誤資訊，並觸發預設值降級
            log.warn("Excel 欄位解析失敗，儲存格位置: [Row:{}, Col:{}], 原因: {}, 已帶入預設值: {}", 
                     cell.getRowIndex() + 1, cell.getColumnIndex() + 1, e.getMessage(), defaultValue);
            return defaultValue;
        }
    }
}