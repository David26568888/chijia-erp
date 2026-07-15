package com.chijia.erp.util;

import java.text.DecimalFormat;

import org.apache.poi.ss.usermodel.*;

public class ExcelHelper {
	/**
	 * 安全地獲取 Excel 儲存格的字串值，防範空值與不同資料格式
	 */
	
	public static String getCellValueAsString(Cell cell) {
		if(cell==null) {
			return "";
		}
		
		switch(cell.getCellType()) {
			case STRING:
				return cell.getStringCellValue().trim();
			case NUMERIC:
				if(DateUtil.isCellDateFormatted(cell)) {
					return cell.getDateCellValue().toString();
				}
				// 避免科學記號（例如電話或統編變成 1.23E+10），使用 DecimalFormat 格式化
				DecimalFormat dFormat = new DecimalFormat("#.##");
				return dFormat.format(cell.getNumericCellValue());
			case BOOLEAN:
				return String.valueOf(cell.getBooleanCellValue());
			case FORMULA:
				try {
					return cell.getStringCellValue().trim();
				}catch(IllegalStateException e){
					return String.valueOf(cell.getNumericCellValue());
					
				}
			case BLANK:
			default:
				return "";
		}
	}
	/**
     * 安全地將儲存格數值轉換為 Double，若失敗或為空則回傳預設值
     */
	public static double getCellValueAsDouble(Cell cell, double defaultValue) {
		if(cell == null) {
			return defaultValue;
		}
		try {
			if(cell.getCellType() == CellType.NUMERIC) {
				return cell.getNumericCellValue();
			}else if(cell.getCellType() == CellType.STRING) {
				String val = cell.getStringCellValue().trim();
				return val.isEmpty()? defaultValue : Double.parseDouble(val);
			}
		}catch(Exception e) {
			// 解析失敗回傳預設值
			System.err.println("解析失敗回傳預設值"+ e.getMessage());
		}
		return defaultValue;
	}
	
}
